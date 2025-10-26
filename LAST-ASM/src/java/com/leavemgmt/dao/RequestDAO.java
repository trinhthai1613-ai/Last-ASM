package com.leavemgmt.dao;

import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.util.DBConnection;
import java.util.Locale;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {
    public static final int OWNER_EDIT_WINDOW_MINUTES = 60;
    // ---------------------------------------------------------
    // 1) Lấy danh sách ĐƠN của CHÍNH MÌNH
    // ---------------------------------------------------------
        public int createRequest(LeaveRequest r) {
        String sql =
            "INSERT INTO dbo.LeaveRequests(LeaveTypeID, Reason, FromDate, ToDate, " +
            "    CreatedByUserID, CurrentStatusID, CreatedAt) " +
            "VALUES (?, ?, ?, ?, ?, (SELECT StatusID FROM dbo.RequestStatuses WHERE StatusCode='INPROGRESS'), GETDATE()); " +
            "SELECT SCOPE_IDENTITY();";
        String auditSql =
            "INSERT INTO dbo.AuditLogs(ActionType, Action, TargetRequestID, ActorUserID, Note) " +
            "VALUES(?, ?, ?, ?, ?);";

        try (Connection cn = DBConnection.getConnection()) {
            cn.setAutoCommit(false);
            int newId = 0;
            try {
                try (PreparedStatement ps = cn.prepareStatement(sql)) {

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
                        if (rs.next()) {
                            newId = rs.getInt(1);
                        }
                    }
                }

                if (newId <= 0) {
                    throw new SQLException("No request id returned");
                }

                try (PreparedStatement psAudit = cn.prepareStatement(auditSql)) {
                    psAudit.setString(1, "CREATE");
                    psAudit.setString(2, "CREATE");
                    psAudit.setInt(3, newId);
                    psAudit.setInt(4, r.getCreatedByUserId());
                    if (r.getReason() == null || r.getReason().isBlank()) {
                        psAudit.setNull(5, Types.NVARCHAR);
                    } else {
                        psAudit.setNString(5, r.getReason());
                    }
                    psAudit.executeUpdate();
                }

                cn.commit();
            } catch (Exception ex) {
                try { cn.rollback(); } catch (SQLException ignore) {}
                throw ex;
            } finally {
                try { cn.setAutoCommit(true); } catch (SQLException ignore) {}
            }
            return newId;
        } catch (Exception e) {
            throw new RuntimeException("createRequest failed", e);
        }
    }

    public void updateRequest(LeaveRequest r, int actorUserId) {
        String sql =
            "UPDATE dbo.LeaveRequests " +
            "SET LeaveTypeID = ?, Reason = ?, FromDate = ?, ToDate = ? " +
            "WHERE RequestID = ? " +
            "  AND CurrentStatusID = (SELECT StatusID FROM dbo.RequestStatuses WHERE StatusCode='INPROGRESS') " +
            "  AND DATEDIFF(MINUTE, CreatedAt, GETDATE()) <= ?";
        String auditSql =
            "INSERT INTO dbo.AuditLogs(ActionType, Action, TargetRequestID, ActorUserID, Note) " +
            "VALUES(?, ?, ?, ?, ?);";

        try (Connection cn = DBConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setInt(1, r.getLeaveTypeId());
                    if (r.getReason() == null || r.getReason().isBlank()) {
                        ps.setNull(2, Types.NVARCHAR);
                    } else {
                        ps.setNString(2, r.getReason());
                    }
                    ps.setDate(3, r.getFromDate());
                    ps.setDate(4, r.getToDate());
                    ps.setInt(5, r.getRequestId());
                    ps.setInt(6, OWNER_EDIT_WINDOW_MINUTES);
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        throw new IllegalStateException("Request can only be edited within one hour while pending.");
                    }
                }

                try (PreparedStatement psAudit = cn.prepareStatement(auditSql)) {
                    psAudit.setString(1, "UPDATE");
                    psAudit.setString(2, "UPDATE");
                    psAudit.setInt(3, r.getRequestId());
                    psAudit.setInt(4, actorUserId);
                    psAudit.setNull(5, Types.NVARCHAR);
                    psAudit.executeUpdate();
                }

                cn.commit();
            } catch (Exception ex) {
                try { cn.rollback(); } catch (SQLException ignore) {}
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                throw new RuntimeException(ex);
            } finally {
                try { cn.setAutoCommit(true); } catch (SQLException ignore) {}
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("updateRequest failed", e);
        }
    }

    public boolean isManagerOf(int managerUserId, int targetUserId) {
        if (managerUserId <= 0 || targetUserId <= 0) return false;

        final String sql =
            "WITH Subtree AS (\n" +
            "    SELECT u.UserID\n" +
            "    FROM dbo.Users u\n" +
            "    WHERE u.UserID = ?\n" +
            "    UNION ALL\n" +
            "    SELECT u2.UserID\n" +
            "    FROM dbo.Users u2\n" +
            "    JOIN Subtree st ON u2.CurrentManagerID = st.UserID\n" +
            ")\n" +
            "SELECT 1 FROM Subtree WHERE UserID = ? OPTION (MAXRECURSION 100);";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

           
            ps.setInt(1, managerUserId);
            ps.setInt(2, targetUserId);

            try (ResultSet rs = ps.executeQuery()) {
          
                return rs.next();
            }
        
        } catch (SQLException e) {
            throw new RuntimeException("isManagerOf failed", e);
        }
       
    }

    public void updateRequest(LeaveRequest r, int actorUserId, String note) {
        String sql =
            "UPDATE dbo.LeaveRequests " +
            "SET LeaveTypeID = ?, Reason = ?, FromDate = ?, ToDate = ? " +
            "WHERE RequestID = ? " +
            "  AND CurrentStatusID = (SELECT StatusID FROM dbo.RequestStatuses WHERE StatusCode='INPROGRESS') " +
            "  AND DATEDIFF(MINUTE, CreatedAt, GETDATE()) <= ?";
        String auditSql =
            "INSERT INTO dbo.AuditLogs(ActionType, Action, TargetRequestID, ActorUserID, Note) " +
            "VALUES(?, ?, ?, ?, ?);";

        try (Connection cn = DBConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setInt(1, r.getLeaveTypeId());
                    if (r.getReason() == null || r.getReason().isBlank()) {
                        ps.setNull(2, Types.NVARCHAR);
                    } else {
                        ps.setNString(2, r.getReason());
                    }
                    ps.setDate(3, r.getFromDate());
                    ps.setDate(4, r.getToDate());
                    ps.setInt(5, r.getRequestId());
                    ps.setInt(6, OWNER_EDIT_WINDOW_MINUTES);
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        throw new IllegalStateException("Request can only be edited within one hour while pending.");
                    }
                }

                try (PreparedStatement psAudit = cn.prepareStatement(auditSql)) {
                    psAudit.setString(1, "UPDATE");
                    psAudit.setString(2, "UPDATE");
                    psAudit.setInt(3, r.getRequestId());
                    psAudit.setInt(4, actorUserId);
                    if (note == null || note.isBlank()) {
                        psAudit.setNull(5, Types.NVARCHAR);
                    } else {
                        psAudit.setNString(5, note.trim());
                    }
                    psAudit.executeUpdate();
                }

                cn.commit();
            } catch (Exception ex) {
                try { cn.rollback(); } catch (SQLException ignore) {}
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                throw new RuntimeException(ex);
            } finally {
                try { cn.setAutoCommit(true); } catch (SQLException ignore) {}
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("updateRequest failed", e);
        }
    }

   

    

  
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

    if (requestId <= 0) {
        throw new IllegalArgumentException("requestId must be positive");
    }
    String normalizedCode = statusCode == null ? null : statusCode.trim().toUpperCase(Locale.ROOT);
    if (normalizedCode == null || normalizedCode.isEmpty()) {
        throw new IllegalArgumentException("statusCode is required");
    }


    final String currentSql =
        "SELECT rs.StatusCode " +
        "FROM dbo.LeaveRequests r " +
        "JOIN dbo.RequestStatuses rs ON rs.StatusID = r.CurrentStatusID " +
        "WHERE r.RequestID = ?";

    final String targetSql =
        "SELECT StatusID, StatusCode " +
        "FROM dbo.RequestStatuses " +
        "WHERE StatusCode = ?";

    final String updateSql =
        "UPDATE dbo.LeaveRequests " +
        "SET CurrentStatusID = ?, UpdatedAt = SYSDATETIME() " +
        "WHERE RequestID = ?";

    final String auditSql =
        "INSERT INTO dbo.AuditLogs(ActionType, Action, TargetRequestID, ActorUserID, Note, OldStatus, NewStatus) " +
        "VALUES(?, ?, ?, ?, ?, ?, ?);";

    try (Connection cn = DBConnection.getConnection()) {
        cn.setAutoCommit(false);
        try {
            String previousStatus = null;
            try (PreparedStatement ps = cn.prepareStatement(currentSql)) {
                ps.setInt(1, requestId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        previousStatus = rs.getString("StatusCode");
                    } else {
                        throw new IllegalArgumentException("Request not found: " + requestId);
                    }
                }
            }

            int newStatusId;
            String dbStatusCode;
            try (PreparedStatement ps = cn.prepareStatement(targetSql)) {
                ps.setString(1, normalizedCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Unknown status code: " + normalizedCode);
                    }
                    newStatusId = rs.getInt("StatusID");
                    dbStatusCode = rs.getString("StatusCode");
                }
            }

            try (PreparedStatement ps = cn.prepareStatement(updateSql)) {
                ps.setInt(1, newStatusId);
                ps.setInt(2, requestId);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("No request updated");
                }
            }


            String action;
            if ("APPROVED".equals(dbStatusCode)) {
                action = "APPROVE";
            } else if ("REJECTED".equals(dbStatusCode)) {
                action = "REJECT";
            } else {
                action = dbStatusCode;
            }

            try (PreparedStatement ps = cn.prepareStatement(auditSql)) {
                ps.setString(1, action);
                ps.setString(2, action);
                ps.setInt(3, requestId);
                if (actorUserId > 0) {
                    ps.setInt(4, actorUserId);
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                 ps.setNull(5, Types.NVARCHAR);
                if (previousStatus == null || previousStatus.isBlank()) {
                    ps.setNull(6, Types.NVARCHAR);
                } else {
                    ps.setNString(6, previousStatus);
                }
                ps.setNString(7, dbStatusCode);
                ps.executeUpdate();
            }

            cn.commit();
        } catch (Exception ex) {
            try { cn.rollback(); } catch (SQLException ignore) {}
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        } finally {
            try { cn.setAutoCommit(true); } catch (SQLException ignore) {}
        }
    } catch (RuntimeException e) {
        throw e;
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
