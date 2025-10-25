package com.leavemgmt.dao;

import com.leavemgmt.util.DBConnection;

import java.sql.*;
import java.util.*;

/**
 * DAO cho màn Agenda (ma trận).
 * - Cây phòng ban dùng Divisions.ParentDivisionID.
 * - Trạng thái đơn lấy từ RequestStatuses.StatusCode (APPROVED / INPROGRESS / REJECTED ...).
 */
public class AgendaDAO {

    /** Kiểm tra user có ở cấp cao nhất (division gốc) không */
    public boolean isTopMostUser(int userId) {
        String sql =
            "SELECT CASE WHEN d.ParentDivisionID IS NULL THEN 1 ELSE 0 END AS IsTop " +
            "FROM dbo.Users u JOIN dbo.Divisions d ON d.DivisionID = u.DivisionID " +
            "WHERE u.UserID = ?";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 1;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return false;
    }

    /** Lấy danh sách thành viên (UserID -> FullName) thuộc subtree của manager */
    public Map<Integer, String> loadTeamMembers(int managerUserId) {
        String sql =
            "WITH SubDiv AS (\n" +
            "  SELECT d.DivisionID\n" +
            "  FROM dbo.Users u\n" +
            "  JOIN dbo.Divisions d ON d.DivisionID = u.DivisionID\n" +
            "  WHERE u.UserID = ?\n" +
            "  UNION ALL\n" +
            "  SELECT c.DivisionID\n" +
            "  FROM dbo.Divisions c\n" +
            "  JOIN SubDiv p ON c.ParentDivisionID = p.DivisionID\n" +
            ")\n" +
            "SELECT u.UserID, u.FullName\n" +
            "FROM dbo.Users u\n" +
            "WHERE u.DivisionID IN (SELECT DivisionID FROM SubDiv)\n" +
            "ORDER BY u.FullName";

        Map<Integer, String> map = new LinkedHashMap<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, managerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("UserID"), rs.getNString("FullName"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    /** Lấy các đơn nghỉ của cả team chồng lấn [from..to] */
    public List<LeaveSpan> loadLeavesForTeam(int managerUserId, java.sql.Date from, java.sql.Date to) {
        String sql =
            "WITH SubDiv AS (\n" +
            "  SELECT d.DivisionID\n" +
            "  FROM dbo.Users u\n" +
            "  JOIN dbo.Divisions d ON d.DivisionID = u.DivisionID\n" +
            "  WHERE u.UserID = ?\n" +
            "  UNION ALL\n" +
            "  SELECT c.DivisionID\n" +
            "  FROM dbo.Divisions c\n" +
            "  JOIN SubDiv p ON c.ParentDivisionID = p.DivisionID\n" +
            ")\n" +
            "SELECT r.CreatedByUserID   AS UserID,\n" +
            "       r.FromDate,\n" +
            "       r.ToDate,\n" +
            "       s.StatusCode         AS StatusCode\n" +
            "FROM dbo.LeaveRequests r\n" +
            "JOIN dbo.RequestStatuses s ON s.StatusID = r.CurrentStatusID\n" +
            "JOIN dbo.Users u           ON u.UserID   = r.CreatedByUserID\n" +
            "WHERE u.DivisionID IN (SELECT DivisionID FROM SubDiv)\n" +
            "  AND r.FromDate <= ? AND r.ToDate >= ?";  // overlap

        List<LeaveSpan> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, managerUserId);
            ps.setDate(2, to);
            ps.setDate(3, from);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int uid            = rs.getInt("UserID");
                    java.sql.Date f    = rs.getDate("FromDate"); // sql.Date => toLocalDate() ok
                    java.sql.Date t    = rs.getDate("ToDate");
                    String statusCode  = rs.getNString("StatusCode"); // APPROVED / INPROGRESS / ...
                    list.add(new LeaveSpan(uid, f, t, normalizeStatus(statusCode)));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    /** Chuẩn hóa: APPROVED -> "approved", còn lại -> "work" (giữ xanh) */
    private String normalizeStatus(String code) {
        if (code == null) return "work";
        String s = code.trim().toUpperCase();
        if ("APPROVED".equals(s))   return "approved";
        return "work"; // INPROGRESS/REJECTED/CANCELLED... => xanh
    }

    /** DTO khoảng nghỉ */
    public static class LeaveSpan {
        public final int userId;
        public final java.sql.Date from;  // sql.Date để gọi toLocalDate()
        public final java.sql.Date to;
        public final String normStatus;   // approved | work
        public LeaveSpan(int userId, java.sql.Date from, java.sql.Date to, String normStatus) {
            this.userId = userId;
            this.from = from;
            this.to = to;
            this.normStatus = normStatus;
        }
    }
}
