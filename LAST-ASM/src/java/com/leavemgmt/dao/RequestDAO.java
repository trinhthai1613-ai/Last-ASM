package com.leavemgmt.dao;

import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.model.LeaveType;
import com.leavemgmt.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {

    // OTHER/Khác xuống cuối
    public List<LeaveType> getLeaveTypes() {
        List<LeaveType> list = new ArrayList<>();
        String sql = "SELECT LeaveTypeID, TypeCode, TypeName " +
                     "FROM dbo.LeaveTypes WHERE IsActive = 1 " +
                     "ORDER BY CASE WHEN TypeCode='OTHER' THEN 1 ELSE 0 END, TypeCode";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new LeaveType(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
            return list;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public LeaveType getLeaveType(int leaveTypeId) {
        String sql = "SELECT LeaveTypeID, TypeCode, TypeName FROM dbo.LeaveTypes WHERE LeaveTypeID = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, leaveTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new LeaveType(rs.getInt(1), rs.getString(2), rs.getString(3));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public String[][] getReasonOptionsNames(int leaveTypeId) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT ReasonOptionID, ReasonName FROM dbo.LeaveReasonOptions " +
                     "WHERE LeaveTypeID=? AND IsActive=1 ORDER BY ReasonCode";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, leaveTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new String[]{ String.valueOf(rs.getInt(1)), rs.getNString(2)});
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list.toArray(new String[0][0]);
    }

    // CORRECT PARAM ORDER: CreatedBy, LeaveType, From, To, Reason, ReasonOptionId
    // 2a) TẠO ĐƠN + GHI AUDIT: CREATE
public int createRequest(int createdByUserId, int leaveTypeId, Integer reasonOptionId,
                         java.sql.Date fromDate, java.sql.Date toDate, String reason) {
    final String CALL = "{call dbo.sp_CreateLeaveRequest(?, ?, ?, ?, ?, ?)}";
    try (java.sql.Connection cn = com.leavemgmt.util.DBConnection.getConnection();
         java.sql.CallableStatement cs = cn.prepareCall(CALL)) {

        // Tham số SP: CreatedBy, LeaveType, From, To, Reason, ReasonOptionId
        cs.setInt(1, createdByUserId);
        cs.setInt(2, leaveTypeId);
        cs.setDate(3, fromDate);
        cs.setDate(4, toDate);
        if (reason == null || reason.isBlank()) cs.setNull(5, java.sql.Types.NVARCHAR);
        else cs.setNString(5, reason);
        if (reasonOptionId == null) cs.setNull(6, java.sql.Types.INTEGER);
        else cs.setInt(6, reasonOptionId);

        int requestId = -1;
        boolean hasResult = cs.execute();
        if (hasResult) {
            try (java.sql.ResultSet rs = cs.getResultSet()) {
                if (rs.next()) {
                    // SP nên trả về cột RequestID
                    requestId = rs.getInt("RequestID");
                }
            }
        }

        // Lấy RequestCode + Status sau khi tạo để log
        String reqCode = null, newStatus = null;
        if (requestId > 0) {
            String q = "SELECT r.RequestCode, s.StatusName " +
                       "FROM dbo.LeaveRequests r JOIN dbo.RequestStatuses s ON s.StatusID = r.CurrentStatusID " +
                       "WHERE r.RequestID = ?";
            try (java.sql.PreparedStatement ps = cn.prepareStatement(q)) {
                ps.setInt(1, requestId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        reqCode = rs.getString(1);
                        newStatus = rs.getString(2);
                    }
                }
            }

            // EntityKey ưu tiên dùng RequestCode, fallback sang requestId
            String entityKey = (reqCode != null && !reqCode.isBlank())
                             ? reqCode : String.valueOf(requestId);

            // Ghi AUDIT (CREATE) — truyền cả ActionType và Action
            String ins = "INSERT INTO dbo.AuditLogs(" +
                         " ActorUserID, ActionType, Action, TargetRequestID, OldStatus, NewStatus, " +
                         " Note, EntityName, EntityKey) " +
                         "VALUES(?, N'CREATE', N'CREATE', ?, NULL, ?, ?, N'LeaveRequest', ?)";
            try (java.sql.PreparedStatement ps = cn.prepareStatement(ins)) {
                ps.setInt(1, createdByUserId);
                ps.setInt(2, requestId);
                ps.setNString(3, newStatus);       // NewStatus
                ps.setNString(4, reason);          // Note
                ps.setNString(5, entityKey);       // EntityKey
                ps.executeUpdate();
            }
        }
        return requestId;

    } catch (java.sql.SQLException e) {
        throw new RuntimeException("Create request failed: " + e.getMessage(), e);
    }
}



    public List<LeaveRequest> listMyRequests(int userId) {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = """
            SELECT r.RequestID, r.RequestCode, t.TypeCode, o.ReasonCode, r.FromDate, r.ToDate,
                   u.FullName AS CreatedBy, s.StatusName, r.DaysBusiness,
                   (SELECT TOP 1 u2.FullName FROM dbo.Users u2 WHERE u2.UserID = r.ApprovedByUserID) AS ProcessedBy
            FROM dbo.LeaveRequests r
            JOIN dbo.Users u ON u.UserID = r.CreatedByUserID
            JOIN dbo.RequestStatuses s ON s.StatusID = r.CurrentStatusID
            JOIN dbo.LeaveTypes t ON t.LeaveTypeID = r.LeaveTypeID
            LEFT JOIN dbo.LeaveReasonOptions o ON o.ReasonOptionID = r.ReasonOptionID
            WHERE r.CreatedByUserID = ?
            ORDER BY r.CreatedAt DESC
        """;
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LeaveRequest lr = new LeaveRequest();
                    lr.setRequestId(rs.getInt("RequestID"));
                    lr.setRequestCode(rs.getString("RequestCode"));
                    lr.setTypeCode(rs.getString("TypeCode"));
                    lr.setReasonCode(rs.getString("ReasonCode"));
                    lr.setFromDate(rs.getDate("FromDate"));
                    lr.setToDate(rs.getDate("ToDate"));
                    lr.setCreatedBy(rs.getString("CreatedBy"));
                    lr.setStatusName(rs.getString("StatusName"));
                    lr.setDaysBusiness((Integer)rs.getObject("DaysBusiness"));
                    lr.setProcessedBy(rs.getString("ProcessedBy"));
                    list.add(lr);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public List<LeaveRequest> listSubtree(int userId, Date from, Date to) {
        List<LeaveRequest> list = new ArrayList<>();
        String call = "{call dbo.sp_GetRequestsForUserAndSubtree(?, ?, ?)}";
        try (Connection cn = DBConnection.getConnection();
             CallableStatement cs = cn.prepareCall(call)) {
            cs.setInt(1, userId);
            cs.setDate(2, from);
            cs.setDate(3, to);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    LeaveRequest lr = new LeaveRequest();
                    lr.setRequestId(rs.getInt("RequestID"));
                    lr.setRequestCode(rs.getString("RequestCode"));
                    lr.setTypeCode(rs.getString("TypeName")); // human-readable
                    lr.setFromDate(rs.getDate("FromDate"));
                    lr.setToDate(rs.getDate("ToDate"));
                    lr.setCreatedBy(rs.getString("CreatedBy"));
                    lr.setStatusName(rs.getString("CurrentStatus"));
                    lr.setDaysBusiness((Integer)rs.getObject("DaysBusiness"));
                    list.add(lr);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public LeaveRequest getRequest(int requestId) {
        String sql = """
            SELECT r.RequestID, r.RequestCode, t.TypeCode, o.ReasonCode, r.FromDate, r.ToDate,
                   u.FullName AS CreatedBy, s.StatusName, r.DaysBusiness,
                   (SELECT TOP 1 u2.FullName FROM dbo.Users u2 WHERE u2.UserID = r.ApprovedByUserID) AS ProcessedBy
            FROM dbo.LeaveRequests r
            JOIN dbo.Users u ON u.UserID = r.CreatedByUserID
            JOIN dbo.RequestStatuses s ON s.StatusID = r.CurrentStatusID
            JOIN dbo.LeaveTypes t ON t.LeaveTypeID = r.LeaveTypeID
            LEFT JOIN dbo.LeaveReasonOptions o ON o.ReasonOptionID = r.ReasonOptionID
            WHERE r.RequestID = ?
        """;
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LeaveRequest lr = new LeaveRequest();
                    lr.setRequestId(rs.getInt("RequestID"));
                    lr.setRequestCode(rs.getString("RequestCode"));
                    lr.setTypeCode(rs.getString("TypeCode"));
                    lr.setReasonCode(rs.getString("ReasonCode"));
                    lr.setFromDate(rs.getDate("FromDate"));
                    lr.setToDate(rs.getDate("ToDate"));
                    lr.setCreatedBy(rs.getString("CreatedBy"));
                    lr.setStatusName(rs.getString("StatusName"));
                    lr.setDaysBusiness((Integer)rs.getObject("DaysBusiness"));
                    lr.setProcessedBy(rs.getString("ProcessedBy"));
                    return lr;
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    // 2b) DUYỆT/TỪ CHỐI + GHI AUDIT: APPROVE/REJECT
public void review(int approverUserId, int requestId, boolean approve, String note) {
    final String CALL = "{call dbo.sp_ReviewLeaveRequest(?, ?, ?, ?)}";
    try (java.sql.Connection cn = com.leavemgmt.util.DBConnection.getConnection()) {

        // Lấy RequestCode + oldStatus TRƯỚC
        String reqCode = null, oldStatus = null;
        String q1 = "SELECT r.RequestCode, s.StatusName " +
                    "FROM dbo.LeaveRequests r JOIN dbo.RequestStatuses s ON s.StatusID = r.CurrentStatusID " +
                    "WHERE r.RequestID = ?";
        try (java.sql.PreparedStatement ps = cn.prepareStatement(q1)) {
            ps.setInt(1, requestId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    reqCode = rs.getString(1);
                    oldStatus = rs.getString(2);
                }
            }
        }

        // Gọi SP duyệt/từ chối
        try (java.sql.CallableStatement cs = cn.prepareCall(CALL)) {
            cs.setInt(1, approverUserId);
            cs.setInt(2, requestId);
            cs.setBoolean(3, approve);
            if (note == null || note.isBlank()) cs.setNull(4, java.sql.Types.NVARCHAR);
            else cs.setNString(4, note);
            cs.execute();
        }

        // Lấy newStatus SAU khi xử lý
        String newStatus = null;
        String q2 = "SELECT s.StatusName " +
                    "FROM dbo.LeaveRequests r JOIN dbo.RequestStatuses s ON s.StatusID = r.CurrentStatusID " +
                    "WHERE r.RequestID = ?";
        try (java.sql.PreparedStatement ps = cn.prepareStatement(q2)) {
            ps.setInt(1, requestId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    newStatus = rs.getString(1);
                }
            }
        }

        String action = approve ? "APPROVE" : "REJECT";
        String entityKey = (reqCode != null && !reqCode.isBlank())
                         ? reqCode : String.valueOf(requestId);

        // Ghi AUDIT (APPROVE/REJECT) — truyền cả ActionType và Action
        String ins = "INSERT INTO dbo.AuditLogs(" +
                     " ActorUserID, ActionType, Action, TargetRequestID, OldStatus, NewStatus, " +
                     " Note, EntityName, EntityKey) " +
                     "VALUES(?, ?, ?, ?, ?, ?, ?, N'LeaveRequest', ?)";
        try (java.sql.PreparedStatement ps = cn.prepareStatement(ins)) {
            ps.setInt(1, approverUserId);
            ps.setNString(2, action);             // ActionType
            ps.setNString(3, action);             // Action
            ps.setInt(4, requestId);
            ps.setNString(5, oldStatus);
            ps.setNString(6, newStatus);
            ps.setNString(7, note);
            ps.setNString(8, entityKey);
            ps.executeUpdate();
        }

    } catch (java.sql.SQLException e) {
        throw new RuntimeException("Review failed: " + e.getMessage(), e);
    }
}

}
