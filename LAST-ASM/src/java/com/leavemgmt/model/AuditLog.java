package com.leavemgmt.model;

import java.sql.Timestamp;

public class AuditLog {
    private int logId;
    private Timestamp occurredAt;
    private String actorName;
    private String actionType;     // CREATE / APPROVE / REJECT
    private String requestCode;    // có thể null
    private String oldStatus;      // có thể null
    private String newStatus;      // có thể null
    private String note;           // có thể null

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }
    public Timestamp getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Timestamp occurredAt) { this.occurredAt = occurredAt; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getRequestCode() { return requestCode; }
    public void setRequestCode(String requestCode) { this.requestCode = requestCode; }
    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
