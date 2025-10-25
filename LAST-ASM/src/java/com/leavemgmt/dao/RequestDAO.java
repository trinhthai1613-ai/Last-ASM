package com.leavemgmt.dao;

import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {

    // ---------------------------------------------------------
    // 1) Lấy danh sách ĐƠN của CHÍNH MÌNH
    // ---------------------------------------------------------
    public List<LeaveRequest> listMyRequests(int userId) {
        List<LeaveRequest> result = new ArrayList<>();

        String sql =
            "SELECT r.RequestID, r.RequestCode, r.LeaveTypeID, lt.TypeName, r.Reason, " +
            "       r.FromDate, r.ToDate, r.CreatedByUserID, u.FullName AS CreatedByName, " +
            "       r.CurrentStatusID, rs.StatusCode, r.CreatedAt " +
            "FROM   dbo.LeaveRequests r " +
            "JOIN   dbo.Users u           ON u.UserID   = r.CreatedByUserID " +
            "JOIN   dbo.RequestStatuses rs ON rs.StatusID = r.CurrentStatusID " +
            "JOIN   dbo.LeaveTypes lt     ON lt.LeaveTypeID = r.LeaveTypeID " +
            "WHERE  r.CreatedByUserID = ? " +
            "ORDER BY r.RequestID DESC";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("listMyRequests failed", e);
        }
        return result;
    }

    // ---------------------------------------------------------
    // 2) Lấy danh sách ĐƠN của CẤP DƯỚI / CÂY PHÒNG BAN (team/subtree)
    //    from..to dùng để khoanh thời gian
    //    NOTE: nếu cột quan hệ quản lý KHÔNG phải 'ManagerUserID', hãy thay
    //    ở hai chỗ comment bên dưới.
    // ---------------------------------------------------------
   // ---- DÁN ĐÈ VÀO RequestDAO.java ----
    // RequestDAO.java
public List<LeaveRequest> listSubtree(int managerUserId, java.sql.Date from, java.sql.Date to) {
    final String sql =
        "WITH Subtree AS (                                                             \n" +
        "    SELECT u.UserID                                                           \n" +
        "    FROM dbo.Users u                                                          \n" +
        "    WHERE u.UserID = ?                                                        \n" +
        "    UNION ALL                                                                 \n" +
        "    SELECT u2.UserID                                                          \n" +
        "    FROM dbo.Users u2                                                         \n" +
        "    JOIN Subtree st ON u2.CurrentManagerID = st.UserID                        \n" +
        ")                                                                              \n" +
        "SELECT r.RequestID, r.RequestCode, r.LeaveTypeID, r.Reason,                   \n" +
        "       r.FromDate, r.ToDate, r.CreatedByUserID, s.StatusCode,                 \n" +
        "       u.FullName AS CreatedByName, lt.TypeName, r.CreatedAt                  \n" +
        "FROM dbo.LeaveRequests r                                                      \n" +
        "JOIN dbo.Users u            ON u.UserID      = r.CreatedByUserID              \n" +
        "JOIN dbo.RequestStatuses s  ON s.StatusID    = r.CurrentStatusID              \n" +
        "JOIN dbo.LeaveTypes lt      ON lt.LeaveTypeID= r.LeaveTypeID                  \n" +
        "WHERE r.FromDate <= ? AND r.ToDate >= ?                                       \n" +
        "  AND r.CreatedByUserID IN (SELECT UserID FROM Subtree)                       \n" +
        "ORDER BY r.RequestID DESC                                                     \n" +
        "OPTION (MAXRECURSION 100);                                                    \n";

    List<LeaveRequest> result = new ArrayList<>();
    try (Connection cn = DBConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {

        ps.setInt(1, managerUserId);  // gốc của cây quản lý
        ps.setDate(2, to);            // điều kiện ngày: FromDate <= to
        ps.setDate(3, from);          //                   ToDate   >= from

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LeaveRequest r = new LeaveRequest();
                r.setRequestId(rs.getInt("RequestID"));
                r.setRequestCode(rs.getString("RequestCode"));
                r.setLeaveTypeId(rs.getInt("LeaveTypeID"));
                r.setReason(rs.getString("Reason"));
                r.setFromDate(rs.getDate("FromDate"));
                r.setToDate(rs.getDate("ToDate"));
                r.setCreatedByUserId(rs.getInt("CreatedByUserID"));
                r.setStatusCode(rs.getString("StatusCode"));     // so sánh bằng CODE
                r.setCreatedByName(rs.getString("CreatedByName"));
                r.setTypeName(rs.getString("TypeName"));
                r.setCreatedAt(rs.getTimestamp("CreatedAt"));
                result.add(r);
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("listSubtree failed (using Users.CurrentManagerID)", e);
    }
    return result;
}


    // ---------------------------------------------------------
    // 3) Lấy 1 đơn theo id (dùng cho Review)
    // ---------------------------------------------------------
public LeaveRequest findById(int id) {
    final String sql =
        "SELECT r.RequestID, r.RequestCode, r.LeaveTypeID, r.Reason, " +
        "       r.FromDate, r.ToDate, r.CreatedByUserID, " +
        "       s.StatusCode, u.FullName AS CreatedByName, lt.TypeName, r.CreatedAt " +
        "FROM dbo.LeaveRequests r " +
        "JOIN dbo.RequestStatuses s ON s.StatusID = r.CurrentStatusID " +
        "JOIN dbo.Users u          ON u.UserID  = r.CreatedByUserID " +
        "JOIN dbo.LeaveTypes lt    ON lt.LeaveTypeID = r.LeaveTypeID " +
        "WHERE r.RequestID = ?";

    try (Connection cn = DBConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setInt(1, id);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                LeaveRequest r = new LeaveRequest();
                r.setRequestId(rs.getInt("RequestID"));
                r.setRequestCode(rs.getString("RequestCode"));
                r.setLeaveTypeId(rs.getInt("LeaveTypeID"));
                r.setReason(rs.getString("Reason"));              // <-- Reason
                r.setFromDate(rs.getDate("FromDate"));
                r.setToDate(rs.getDate("ToDate"));
                r.setCreatedByUserId(rs.getInt("CreatedByUserID"));
                r.setStatusCode(rs.getString("StatusCode"));       // <-- Code
                r.setCreatedByName(rs.getString("CreatedByName"));
                r.setTypeName(rs.getString("TypeName"));
                r.setCreatedAt(rs.getTimestamp("CreatedAt"));
                return r;
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("findById failed", e);
    }
    return null;
}

    // ---------------------------------------------------------
    // 4) Tạo đơn
    // ---------------------------------------------------------
    public int createRequest(LeaveRequest r) {
        String sql =
            "INSERT INTO dbo.LeaveRequests(LeaveTypeID, Reason, FromDate, ToDate, " +
            "    CreatedByUserID, CurrentStatusID, CreatedAt) " +
            "VALUES (?, ?, ?, ?, ?, (SELECT StatusID FROM dbo.RequestStatuses WHERE StatusCode='INPROGRESS'), GETDATE()); " +
            "SELECT SCOPE_IDENTITY();";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, r.getLeaveTypeId());
            if (r.getReason() == null || r.getReason().isBlank()) {
                ps.setNull(2, Types.NVARCHAR);
            } else {
                ps.setNString(2, r.getReason());
            }
            ps.setDate(3, r.getFromDate());
            ps.setDate(4, r.getToDate());
            ps.setInt(5, r.getCreatedByUserId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("createRequest failed", e);
        }
        return 0;
    }
    // Lấy danh sách loại nghỉ để render combobox
public java.util.List<com.leavemgmt.model.LeaveType> listTypes() {
    java.util.List<com.leavemgmt.model.LeaveType> list = new java.util.ArrayList<>();
    String sql = "SELECT LeaveTypeID, TypeName FROM dbo.LeaveTypes ORDER BY LeaveTypeID";
    try (java.sql.Connection cn = DBConnection.getConnection();
         java.sql.PreparedStatement ps = cn.prepareStatement(sql);
         java.sql.ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            com.leavemgmt.model.LeaveType t = new com.leavemgmt.model.LeaveType();
            t.setLeaveTypeId(rs.getInt("LeaveTypeID"));
            t.setTypeName(rs.getString("TypeName"));
            list.add(t);
        }
    } catch (Exception ex) {
        throw new RuntimeException("listTypes failed", ex);
    }
    return list;
}

// Cập nhật trạng thái bằng STATUS CODE (APPROVED/REJECTED/INPROGRESS)
public void updateStatusByCode(int requestId, String statusCode, int actorUserId, String note) {
    String sql =
            "UPDATE r SET r.CurrentStatusID = s.StatusID " +
            "FROM dbo.LeaveRequests r " +
            "JOIN dbo.RequestStatuses s ON s.StatusCode = ? " +
            "WHERE r.RequestID = ?; " +
            // nếu bạn có bảng log, thêm insert ở dưới (tùy DB của bạn):
            // "INSERT INTO dbo.AuditLogs(ActionType, TargetRequestID, ActorUserID, Note) " +
            // "VALUES('REVIEW', ?, ?, ?);"
            "";

    try (java.sql.Connection cn = DBConnection.getConnection();
         java.sql.PreparedStatement ps = cn.prepareStatement(sql)) {

        ps.setString(1, statusCode);
        ps.setInt(2, requestId);
        ps.executeUpdate();

        // Nếu có bảng log riêng, bỏ comment và set tiếp:
        // try (java.sql.PreparedStatement ps2 = cn.prepareStatement(
        //     "INSERT INTO dbo.AuditLogs(ActionType, TargetRequestID, ActorUserID, Note) VALUES(?,?,?,?)")) {
        //     ps2.setString(1, "REVIEW");
        //     ps2.setInt(2, requestId);
        //     ps2.setInt(3, actorUserId);
        //     ps2.setString(4, (note == null || note.isBlank()) ? null : note.trim());
        //     ps2.executeUpdate();
        // }

    } catch (Exception ex) {
        throw new RuntimeException("updateStatusByCode failed", ex);
    }
}

    // ---------------------------------------------------------
    // Helper: map 1 dòng ResultSet -> LeaveRequest
    // ---------------------------------------------------------
    private LeaveRequest mapRow(ResultSet rs) throws SQLException {
        LeaveRequest r = new LeaveRequest();
        r.setRequestId(rs.getInt("RequestID"));
        r.setRequestCode(rs.getString("RequestCode"));
        r.setLeaveTypeId(rs.getInt("LeaveTypeID"));
        try { r.setTypeName(rs.getString("TypeName")); } catch (Exception ignore) {}
        r.setReason(rs.getString("Reason"));
        r.setFromDate(rs.getDate("FromDate"));
        r.setToDate(rs.getDate("ToDate"));
        r.setCreatedByUserId(rs.getInt("CreatedByUserID"));
        try { r.setCreatedByName(rs.getString("CreatedByName")); } catch (Exception ignore) {}
        r.setCurrentStatusId(rs.getInt("CurrentStatusID"));
        r.setStatusCode(rs.getString("StatusCode"));
        try { r.setCreatedAt(rs.getTimestamp("CreatedAt")); } catch (Exception ignore) {}
        // BizDays để model tự tính (getBizDays) nếu không có cột từ DB
        return r;
    }
}
