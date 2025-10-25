package com.leavemgmt.model;

import java.sql.Timestamp;

public class AuditLog {
    private Timestamp occurredAt;
    private String actorName;
    private String actionType;

    private Integer requestId;      // dùng khi không có RequestCode
    private String requestCode;     // LR000001 ...
    private String oldStatus;       // tên hiển thị (có thể null)
    private String newStatus;       // tên hiển thị (có thể null)
    private String currentStatusCode; // <-- MỚI: INPROGRESS/APPROVED/REJECTED
    private String note;

    // ===== getters/setters =====
    public Timestamp getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Timestamp occurredAt) { this.occurredAt = occurredAt; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }

    public String getRequestCode() { return requestCode; }
    public void setRequestCode(String requestCode) { this.requestCode = requestCode; }

    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getCurrentStatusCode() { return currentStatusCode; }
    public void setCurrentStatusCode(String currentStatusCode) { this.currentStatusCode = currentStatusCode; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    // ===== Helper để render cột Status trên JSP =====
    public String getStatusCell() {
        if (oldStatus != null && newStatus != null && !oldStatus.trim().isEmpty() && !newStatus.trim().isEmpty()) {
            // log APPROVE/REJECT: có Old → New
            return oldStatus + " → " + newStatus;
        }
        if (currentStatusCode != null) {
            switch (currentStatusCode) {
                case "INPROGRESS": return "Đang xử lý";
                case "APPROVED":   return "Đã duyệt";
                case "REJECTED":   return "Từ chối";
                default:           return currentStatusCode; // fallback
            }
        }
        return "";
    }
}
