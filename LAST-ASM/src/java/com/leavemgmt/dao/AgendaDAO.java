package com.leavemgmt.dao;

import com.leavemgmt.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AgendaDAO {
    public List<String[]> listDivisions() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT DivisionID, DivisionName FROM dbo.Divisions WHERE IsActive = 1 ORDER BY DivisionName";
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new String[]{String.valueOf(rs.getInt(1)), rs.getNString(2)});
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<String[]> agendaByDivision(int divisionId, Date from, Date to) {
        List<String[]> rows = new ArrayList<>();
        String call = "{call dbo.sp_AgendaByDivision(?, ?, ?)}";
        try (Connection cn = DBConnection.getConnection();
             CallableStatement cs = cn.prepareCall(call)) {
            cs.setInt(1, divisionId);
            cs.setDate(2, from);
            cs.setDate(3, to);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{ rs.getNString("FullName"),
                                           String.valueOf(rs.getDate("WorkingDate")),
                                           rs.getNString("Attendance")});
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }
}
