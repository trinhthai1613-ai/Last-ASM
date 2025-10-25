package com.leavemgmt.dao;

import com.leavemgmt.model.User;
import com.leavemgmt.util.DBConnection;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class UserDAO {

    public User login(String username, String passwordPlain) {
        String sql =
            "SELECT u.UserID, u.Username, u.FullName, u.Email, " +
            "       u.DivisionID, d.DivisionName, u.CurrentManagerID " +
            "FROM dbo.Users u JOIN dbo.Divisions d ON d.DivisionID = u.DivisionID " +
            "WHERE u.Username = ? AND u.IsActive = 1 AND u.PasswordHash = ?";

        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, passwordPlain);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                User u = new User();
                u.setUserId(rs.getInt("UserID"));
                u.setUsername(rs.getString("Username"));
                u.setFullName(rs.getString("FullName"));
                u.setEmail(rs.getString("Email"));
                u.setDivisionId(rs.getInt("DivisionID"));
                u.setDivisionName(rs.getString("DivisionName"));
                u.setCurrentManagerId((Integer) rs.getObject("CurrentManagerID"));

                u.setRoleCodes(fetchRoleCodes(u.getUserId(), cn));
                u.setHasSubordinates(checkHasSubordinates(u.getUserId(), cn));
                return u;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    private Set<String> fetchRoleCodes(int userId, Connection cn) throws SQLException {
        Set<String> roles = new HashSet<>();
        String sql = "SELECT r.RoleCode " +
                     "FROM dbo.UserRoles ur JOIN dbo.Roles r ON r.RoleID = ur.RoleID " +
                     "WHERE ur.UserID = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) roles.add(rs.getString(1));
            }
        }
        return roles;
    }

    private boolean checkHasSubordinates(int userId, Connection cn) throws SQLException {
        String sql = "SELECT TOP 1 1 FROM dbo.Users WHERE IsActive=1 AND CurrentManagerID = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // có ít nhất 1 cấp dưới
            }
        }
    }
}
