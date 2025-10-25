package com.leavemgmt.dao;

import com.leavemgmt.model.LeaveType;
import com.leavemgmt.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaveTypeDAO {
    public List<LeaveType> listActive() {
        String sql = "SELECT LeaveTypeID, TypeCode, TypeName " +
                     "FROM dbo.LeaveTypes " +
                     "WHERE IsActive = 1 " +
                     "ORDER BY CASE WHEN TypeCode = 'OTHER' THEN 1 ELSE 0 END, TypeName";
        List<LeaveType> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LeaveType t = new LeaveType();
                t.setLeaveTypeId(rs.getInt("LeaveTypeID"));
                t.setTypeCode(rs.getString("TypeCode"));
                t.setTypeName(rs.getNString("TypeName"));
                list.add(t);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
