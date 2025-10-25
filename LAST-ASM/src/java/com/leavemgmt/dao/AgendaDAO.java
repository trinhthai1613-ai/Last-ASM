package com.leavemgmt.dao;

import com.leavemgmt.util.DBConnection;

import java.sql.*;
import java.util.*;

/**
 * DAO đơn giản cho Agenda.
 * Chú ý: câu SQL giả định bạn đã có quan hệ subtree như trong RequestDAO.listSubtree().
 * Nếu bạn đã có view/function trả về subtree, có thể tái dùng ở đây.
 */
public class AgendaDAO {

    /** Trả về danh sách nhân sự (UserID -> FullName) trong subtree của manager */
    public Map<Integer, String> loadTeamMembers(int managerUserId) {
        String sql =
            // Lấy tất cả users thuộc cùng nhánh (manager + cấp dưới)
            "WITH SubDiv AS (\n" +
            "  SELECT d.DivisionID FROM dbo.Users u JOIN dbo.Divisions d ON d.DivisionID = u.DivisionID WHERE u.UserID=?\n" +
            "  UNION ALL\n" +
            "  SELECT c.DivisionID FROM dbo.Divisions c JOIN SubDiv p ON c.ParentID = p.DivisionID\n" +
            ")\n" +
            "SELECT u.UserID, u.FullName\n" +
            "FROM dbo.Users u WHERE u.DivisionID IN (SELECT DivisionID FROM SubDiv)\n" +
            "ORDER BY u.FullName";

        Map<Integer, String> map = new LinkedHashMap<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, managerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getInt("UserID"), rs.getNString("FullName"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    /** Trả về các khoảng nghỉ của cả team trùng với [from..to] */
    public List<LeaveSpan> loadLeavesForTeam(int managerUserId, Date from, Date to) {
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
            "  AND r.FromDate <= ? AND r.ToDate >= ?"; // overlap

        List<LeaveSpan> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, managerUserId);
            ps.setDate(2, to);
            ps.setDate(3, from);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int uid = rs.getInt("UserID");
                    Date f = rs.getDate("FromDate");
                    Date t = rs.getDate("ToDate");
                    String status = normalizeStatus(rs.getString("StatusName"));
                    list.add(new LeaveSpan(uid, f, t, status));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private String normalizeStatus(String raw) {
        if (raw == null) return "pending";
        String s = raw.trim().toLowerCase();
        if (s.contains("duyệt") || s.equals("approved") || s.equals("da duyet")) return "approved";
        if (s.contains("xử lý") || s.equals("pending") || s.equals("inprogress") || s.equals("dang xu ly")) return "pending";
        return "pending";
    }

    /** DTO nhỏ gọn */
    public static class LeaveSpan {
        public final int userId;
        public final Date from;
        public final Date to;
        public final String normStatus; // approved|pending
        public LeaveSpan(int userId, Date from, Date to, String normStatus) {
            this.userId = userId; this.from = from; this.to = to; this.normStatus = normStatus;
        }
    }
}
