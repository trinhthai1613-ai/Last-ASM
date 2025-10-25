package com.leavemgmt.dao;

import com.leavemgmt.util.DBConnection;

import java.sql.*;
import java.util.*;

/**
 * DAO phục vụ Agenda.
 * CTE SubDiv dựa trên cây Divisions(ParentID). Điều chỉnh nếu schema khác.
 */
public class AgendaDAO {

    /** Lấy danh sách thành viên (UserID -> FullName) thuộc subtree của manager */
    public Map<Integer, String> loadTeamMembers(int managerUserId) {
        String sql =
            "WITH SubDiv AS (\n" +
            "  SELECT d.DivisionID FROM dbo.Users u JOIN dbo.Divisions d ON d.DivisionID = u.DivisionID WHERE u.UserID=?\n" +
            "  UNION ALL\n" +
            "  SELECT c.DivisionID FROM dbo.Divisions c JOIN SubDiv p ON c.ParentID = p.DivisionID\n" +
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
            "  SELECT d.DivisionID FROM dbo.Users u JOIN dbo.Divisions d ON d.DivisionID = u.DivisionID WHERE u.UserID=?\n" +
            "  UNION ALL\n" +
            "  SELECT c.DivisionID FROM dbo.Divisions c JOIN SubDiv p ON c.ParentID = p.DivisionID\n" +
            ")\n" +
            "SELECT r.CreatedByUserID AS UserID, r.FromDate, r.ToDate, r.StatusName\n" +
            "FROM dbo.LeaveRequests r\n" +
            "JOIN dbo.Users u ON u.UserID = r.CreatedByUserID\n" +
            "WHERE u.DivisionID IN (SELECT DivisionID FROM SubDiv)\n" +
            "  AND r.FromDate <= ? AND r.ToDate >= ?";   // overlap

        List<LeaveSpan> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, managerUserId);
            ps.setDate(2, to);
            ps.setDate(3, from);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int uid = rs.getInt("UserID");
                    java.sql.Date f = rs.getDate("FromDate"); // java.sql.Date
                    java.sql.Date t = rs.getDate("ToDate");   // java.sql.Date
                    String status = normalizeStatus(rs.getString("StatusName"));
                    list.add(new LeaveSpan(uid, f, t, status));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    /** Chuẩn hóa trạng thái về approved | pending */
    private String normalizeStatus(String raw) {
        if (raw == null) return "pending";
        String s = raw.trim().toLowerCase();
        if (s.contains("duyệt") || s.equals("approved") || s.equals("đã duyệt") || s.equals("da duyet"))
            return "approved";
        if (s.contains("xử lý") || s.equals("inprogress") || s.equals("pending") || s.equals("dang xu ly"))
            return "pending";
        return "pending";
    }

    /** DTO khoảng nghỉ */
    public static class LeaveSpan {
        public final int userId;
        public final java.sql.Date from;  // dùng java.sql.Date để gọi toLocalDate()
        public final java.sql.Date to;
        public final String normStatus;
        public LeaveSpan(int userId, java.sql.Date from, java.sql.Date to, String normStatus) {
            this.userId = userId;
            this.from = from;
            this.to = to;
            this.normStatus = normStatus;
        }
    }
}
