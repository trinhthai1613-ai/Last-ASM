package com.leavemgmt.dao;

import com.leavemgmt.model.AuditLog;
import com.leavemgmt.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {
    public List<AuditLog> listAll() {
        String sql = """
          SELECT al.LogID, al.OccurredAt, al.ActionType, al.Note,
                 u.FullName AS ActorName,
                 r.RequestCode,
                 al.EntityKey,                -- đọc thêm
                 al.OldStatus, al.NewStatus
          FROM dbo.AuditLogs al
          LEFT JOIN dbo.Users u ON u.UserID = al.ActorUserID
          LEFT JOIN dbo.LeaveRequests r ON r.RequestID = al.TargetRequestID
          ORDER BY al.OccurredAt DESC, al.LogID DESC
        """;

        List<AuditLog> list = new ArrayList<>();
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AuditLog a = new AuditLog();
                a.setLogId(rs.getInt("LogID"));
                a.setOccurredAt(rs.getTimestamp("OccurredAt"));
                a.setActionType(rs.getString("ActionType"));
                a.setNote(rs.getNString("Note"));
                a.setActorName(rs.getNString("ActorName"));
                a.setRequestCode(rs.getString("RequestCode"));
                a.setEntityKey(rs.getString("EntityKey"));          // gán vào model
                a.setOldStatus(rs.getNString("OldStatus"));
                a.setNewStatus(rs.getNString("NewStatus"));
                list.add(a);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
