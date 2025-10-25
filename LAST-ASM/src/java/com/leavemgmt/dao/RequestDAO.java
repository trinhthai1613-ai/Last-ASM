package com.leavemgmt.dao;

import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {

    // --- CREATE ---
    public int createRequest(LeaveRequest req) {
        String sql =
            "INSERT INTO dbo.LeaveRequests(" +
            "  LeaveTypeID, Reason, FromDate, ToDate, CreatedByUserID, " +
            "  CurrentStatusID, CreatedAt" +
            ") VALUES (" +
            "  ?, ?, ?, ?, ?, " +
            "  (SELECT StatusID FROM dbo.RequestStatuses WHERE StatusCode='INPROGRESS'), " +
            "  SYSDATETIME()" +
            ");";

        int newId = 0;

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, req.getLeaveTypeId());
            ps.setNString(2, (req.getReason()!=null && !req.getReason().isBlank()) ? req.getReason() : null);
            ps.setDate(3, req.getFromDate());
            ps.setDate(4, req.getToDate());
            ps.setInt(5, req.getCreatedByUserId());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) newId = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("createRequest failed", e);
        }

        if (newId > 0) {
            String upCode =
                "UPDATE dbo.LeaveRequests " +
                "SET RequestCode = 'LR' + RIGHT('000000' + CAST(RequestID AS VARCHAR(6)), 6) " +
                "WHERE RequestID = ?";
            try (Connection cn = DBConnection.getConnection();
                 PreparedStatement ps = cn.prepareStatement(upCode)) {
                ps.setInt(1, newId);
                ps.executeUpdate();
            } catch (SQLException ignore) {}
        }

        return newId;
    }

    // --- UPDATE STATUS bằng CODE (APPROVED/REJECTED/...) ---
    public void updateStatusByCode(int requestId, String statusCode) {
        String sql =
            "UPDATE dbo.LeaveRequests " +
            "SET CurrentStatusID = (SELECT StatusID FROM dbo.RequestStatuses WHERE StatusCode = ?) " +
            "WHERE RequestID = ?";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, statusCode);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateStatusByCode failed", e);
        }
    }

    // --- FIND ONE ---
    public LeaveRequest findById(int requestId) {
        String sql =
            "SELECT r.RequestID, r.RequestCode, r.LeaveTypeID, lt.TypeName, r.Reason, " +
            "       r.FromDate, r.ToDate, s.StatusCode, r.CreatedByUserID, u.FullName AS CreatedByName " +
            "FROM dbo.LeaveRequests r " +
            "JOIN dbo.LeaveTypes lt     ON lt.LeaveTypeID = r.LeaveTypeID " +
            "JOIN dbo.RequestStatuses s  ON s.StatusID     = r.CurrentStatusID " +
            "JOIN dbo.Users u            ON u.UserID       = r.CreatedByUserID " +
            "WHERE r.RequestID = ?";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
        return null;
    }

    // --- LIST MINE ---
    public List<LeaveRequest> listMyRequests(int userId) {
        String sql =
            "SELECT r.RequestID, r.RequestCode, r.LeaveTypeID, lt.TypeName, r.Reason, " +
            "       r.FromDate, r.ToDate, s.StatusCode, r.CreatedByUserID, u.FullName AS CreatedByName " +
            "FROM dbo.LeaveRequests r " +
            "JOIN dbo.LeaveTypes lt     ON lt.LeaveTypeID = r.LeaveTypeID " +
            "JOIN dbo.RequestStatuses s  ON s.StatusID     = r.CurrentStatusID " +
            "JOIN dbo.Users u            ON u.UserID       = r.CreatedByUserID " +
            "WHERE r.CreatedByUserID = ? " +
            "ORDER BY r.RequestID DESC";

        List<LeaveRequest> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("listMyRequests failed", e);
        }
        return list;
    }

    // --- LIST SUBTREE (tuỳ logic cây phòng ban của bạn) ---
    public List<LeaveRequest> listSubtree(int managerUserId, Date from, Date to) {
        // Ở đây giả sử bạn đã có sẵn view/TVF trả về danh sách userId trong cây.
        // Nếu bạn đã có câu SQL cũ thì chỉ cần thêm JOIN RequestStatuses và SELECT s.StatusCode.
        String sql =
            "SELECT r.RequestID, r.RequestCode, r.LeaveTypeID, lt.TypeName, r.Reason, " +
            "       r.FromDate, r.ToDate, s.StatusCode, r.CreatedByUserID, u.FullName AS CreatedByName " +
            "FROM dbo.LeaveRequests r " +
            "JOIN dbo.LeaveTypes lt     ON lt.LeaveTypeID = r.LeaveTypeID " +
            "JOIN dbo.RequestStatuses s  ON s.StatusID     = r.CurrentStatusID " +
            "JOIN dbo.Users u            ON u.UserID       = r.CreatedByUserID " +
            "JOIN dbo.Users tree         ON tree.UserID    = r.CreatedByUserID " +
            "WHERE tree.ManagerChain LIKE '%,' + CAST(? AS VARCHAR(10)) + ',%' " + 
            "  AND r.FromDate >= ? AND r.ToDate <= ? " +
            "ORDER BY r.RequestID DESC";

        List<LeaveRequest> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, managerUserId);
            ps.setDate(2, from);
            ps.setDate(3, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("listSubtree failed", e);
        }
        return list;
    }

    // --- map row ---
    private LeaveRequest mapRow(ResultSet rs) throws SQLException {
        LeaveRequest lr = new LeaveRequest();
        lr.setRequestId(rs.getInt("RequestID"));
        lr.setRequestCode(rs.getString("RequestCode"));
        lr.setLeaveTypeId(rs.getInt("LeaveTypeID"));
        lr.setLeaveTypeName(rs.getString("TypeName"));
        lr.setReason(rs.getNString("Reason"));
        lr.setFromDate(rs.getDate("FromDate"));
        lr.setToDate(rs.getDate("ToDate"));
        lr.setStatusCode(rs.getString("StatusCode"));      // <-- chỉ dùng CODE
        lr.setCreatedByUserId(rs.getInt("CreatedByUserID"));
        lr.setCreatedByName(rs.getNString("CreatedByName"));
        return lr;
    }
}
