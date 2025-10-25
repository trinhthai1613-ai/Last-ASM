package com.leavemgmt.dao;

import com.leavemgmt.model.AuditLog;
import com.leavemgmt.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {
public List<AuditLog> listAll() {
    String sql =
        "SELECT a.LogID, a.OccurredAt, a.Action AS ActionType, a.Note, " +
        "       u.FullName AS ActorName, " +
        "       r.RequestID, r.RequestCode, " +
        "       s.StatusCode AS CurrentStatusCode, " +        // <-- lấy code qua JOIN
        "       a.OldStatus, a.NewStatus " +
        "FROM dbo.AuditLogs a " +
        "LEFT JOIN dbo.Users u           ON u.UserID = a.ActorUserID " +
        "LEFT JOIN dbo.LeaveRequests r   ON r.RequestID = a.TargetRequestID " +
        "LEFT JOIN dbo.RequestStatuses s  ON s.StatusID = r.CurrentStatusID " + // <-- JOIN đúng
        "ORDER BY a.LogID DESC";

    List<AuditLog> list = new ArrayList<>();
    try (Connection cn = DBConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            AuditLog a = new AuditLog();

            a.setOccurredAt(rs.getTimestamp("OccurredAt"));
            a.setActionType(rs.getString("ActionType"));
            a.setNote(rs.getString("Note"));
            a.setActorName(rs.getString("ActorName"));

            // Request code: ưu tiên dùng cột, nếu null thì format từ ID
            Integer rid   = (Integer) rs.getObject("RequestID"); // có thể null
            String reqCode = rs.getString("RequestCode");
            if (reqCode == null && rid != null) {
                reqCode = String.format("LR%06d", rid);
            }
            a.setRequestId(rid);
            a.setRequestCode(reqCode);

            a.setOldStatus(rs.getString("OldStatus"));
            a.setNewStatus(rs.getString("NewStatus"));

            // alias phải trùng 'CurrentStatusCode'
            a.setCurrentStatusCode(rs.getString("CurrentStatusCode"));

            list.add(a);
        }
    } catch (Exception e) {
        throw new RuntimeException("listAudit failed", e);
    }
    return list;
}

}
