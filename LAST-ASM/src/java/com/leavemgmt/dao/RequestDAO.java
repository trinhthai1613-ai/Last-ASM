package com.leavemgmt.dao;

import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {
    private static final String MANAGER_COL = "CurrentManagerID";
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
    "WITH Tree AS (                                                    \n" +
    "    SELECT u.UserID                                              \n" +
    "    FROM dbo.Users u                                             \n" +
    "    WHERE u.UserID = ?                                           \n" +
    "    UNION ALL                                                    \n" +
    "    SELECT c.UserID                                              \n" +
    "    FROM dbo.Users c                                             \n" +
    "    JOIN Tree t ON c." + MANAGER_COL + " = t.UserID              \n" +
    ")                                                                 \n" +
    "SELECT r.RequestID, r.RequestCode, r.LeaveTypeID,                 \n" +
    "       r.Reason, r.FromDate, r.ToDate,                            \n" +
    "       r.CreatedByUserID, r.CurrentStatusID,                      \n" +
    "       rs.StatusCode, u.FullName AS CreatedByName, lt.TypeName,   \n" +
    "       r.CreatedAt                                                \n" +
    "FROM dbo.LeaveRequests r                                          \n" +
    "JOIN dbo.Users u        ON u.UserID = r.CreatedByUserID           \n" +
    "JOIN Tree t            ON t.UserID = u.UserID                     \n" +
    "JOIN dbo.RequestStatuses rs ON rs.StatusID    = r.CurrentStatusID \n" +
    "JOIN dbo.LeaveTypes lt   ON lt.LeaveTypeID = r.LeaveTypeID        \n" +
    "WHERE r.FromDate <= ? AND r.ToDate >= ?                           \n" +
    "ORDER BY r.RequestID DESC                                         \n" +
    "OPTION (MAXRECURSION 100);";

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
    // RequestDAO.java
public List<LeaveRequest> listSubtree(int managerUserId, java.sql.Date from, java.sql.Date to) {
    List<LeaveRequest> result = new ArrayList<>();

    String sql =
        "WITH Tree AS (                                                  \n" +
        "   SELECT u.UserID                                              \n" +
        "   FROM dbo.Users u                                             \n" +
        "   WHERE u.UserID = ?                                           \n" +
        "   UNION ALL                                                    \n" +
        "   SELECT c.UserID                                              \n" +
        "   FROM dbo.Users c                                             \n" +
        "   JOIN Tree t ON c.CurrentManagerID = t.UserID                    \n" +
        ")                                                               \n" +
        "SELECT r.RequestID, r.RequestCode, r.LeaveTypeID,               \n" +
        "       r.Reason, r.FromDate, r.ToDate,                          \n" +
        "       r.CreatedByUserID, r.CurrentStatusID,                    \n" +
        "       rs.StatusCode,                                           \n" +
        "       u.FullName AS CreatedByName,                             \n" +
        "       lt.TypeName,                                             \n" +
        "       r.CreatedAt                                              \n" +
        "FROM dbo.LeaveRequests r                                        \n" +
        "JOIN dbo.Users u ON u.UserID = r.CreatedByUserID                \n" +
        "JOIN Tree t   ON t.UserID = u.UserID                            \n" +
        "JOIN dbo.RequestStatuses rs ON rs.StatusID = r.CurrentStatusID  \n" +
        "JOIN dbo.LeaveTypes lt ON lt.LeaveTypeID = r.LeaveTypeID        \n" +
        "WHERE r.FromDate <= ? AND r.ToDate >= ?                         \n" +
        "ORDER BY r.RequestID DESC                                       \n" +
        "OPTION (MAXRECURSION 100);";

    try (Connection cn = DBConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {

        ps.setInt(1, managerUserId);
        ps.setDate(2, from);
        ps.setDate(3, to);

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
                r.setCurrentStatusId(rs.getInt("CurrentStatusID"));
                r.setStatusCode(rs.getString("StatusCode"));
                r.setCreatedByName(rs.getString("CreatedByName"));
                r.setLeaveTypeName(rs.getString("TypeName"));
                r.setCreatedAt(rs.getTimestamp("CreatedAt"));
                result.add(r);
            }
        }
    } catch (Exception e) {
        throw new RuntimeException("listSubtree failed", e);
    }
    return result;
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
