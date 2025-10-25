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
    public List<LeaveRequest> listSubtree(int managerUserId, Date from, Date to) {
        List<LeaveRequest> result = new ArrayList<>();

        String sql =
            "WITH UserTree AS (                                           \n" +
            "    SELECT u.UserID                                          \n" +
            "    FROM   dbo.Users u                                        \n" +
            "    WHERE  u.UserID = ?                                       \n" +
            "    UNION ALL                                                 \n" +
            "    SELECT c.UserID                                           \n" +
            "    FROM   dbo.Users c                                        \n" +
            "    JOIN   UserTree p ON c.ManagerUserID = p.UserID           \n" + // <--- ĐỔI 'ManagerUserID' nếu DB khác
            ")                                                             \n" +
            "SELECT r.RequestID, r.RequestCode, r.LeaveTypeID, lt.TypeName, r.Reason, \n" +
            "       r.FromDate, r.ToDate, r.CreatedByUserID, u.FullName AS CreatedByName, \n" +
            "       r.CurrentStatusID, rs.StatusCode, r.CreatedAt          \n" +
            "FROM   dbo.LeaveRequests r                                    \n" +
            "JOIN   dbo.Users u           ON u.UserID   = r.CreatedByUserID \n" +
            "JOIN   dbo.RequestStatuses rs ON rs.StatusID = r.CurrentStatusID \n" +
            "JOIN   dbo.LeaveTypes lt     ON lt.LeaveTypeID = r.LeaveTypeID \n" +
            "JOIN   UserTree t           ON t.UserID  = r.CreatedByUserID    \n" +
            "WHERE  r.FromDate <= ? AND r.ToDate >= ?                        \n" +
            "ORDER BY r.RequestID DESC                                      \n" +
            "OPTION (MAXRECURSION 100)";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, managerUserId);
            ps.setDate(2, from);
            ps.setDate(3, to);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("listSubtree failed", e);
        }
        return result;
    }

    // ---------------------------------------------------------
    // 3) Lấy 1 đơn theo id (dùng cho Review)
    // ---------------------------------------------------------
    public LeaveRequest findById(int id) {
        String sql =
            "SELECT r.RequestID, r.RequestCode, r.LeaveTypeID, lt.TypeName, r.Reason, " +
            "       r.FromDate, r.ToDate, r.CreatedByUserID, u.FullName AS CreatedByName, " +
            "       r.CurrentStatusID, rs.StatusCode, r.CreatedAt " +
            "FROM   dbo.LeaveRequests r " +
            "JOIN   dbo.Users u           ON u.UserID   = r.CreatedByUserID " +
            "JOIN   dbo.RequestStatuses rs ON rs.StatusID = r.CurrentStatusID " +
            "JOIN   dbo.LeaveTypes lt     ON lt.LeaveTypeID = r.LeaveTypeID " +
            "WHERE  r.RequestID = ?";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
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
