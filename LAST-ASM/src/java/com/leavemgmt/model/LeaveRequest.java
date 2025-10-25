package com.leavemgmt.model;

import java.sql.Date;
import java.sql.Timestamp;

public class LeaveRequest {

    // PK, mã hiển thị
    private int requestId;
    private String requestCode;

    // Loại phép & lý do
    private int leaveTypeId;
    private String reason;

    // Khoảng thời gian
    private Date fromDate;
    private Date toDate;

    // Người tạo
    private int createdByUserId;

    // --- Trạng thái --- (MỚI)
    private int currentStatusId; // id trong bảng RequestStatuses
    private String statusCode;   // INPROGRESS / APPROVED / REJECTED

    // --- Dùng cho hiển thị (MỚI hoặc đã có) ---
    private String createdByName;
    private String leaveTypeName;
    private Timestamp createdAt;

    /* ====== Getters/Setters ====== */

    public int getRequestId() {
        return requestId;
    }
    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getRequestCode() {
        return requestCode;
    }
    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public int getLeaveTypeId() {
        return leaveTypeId;
    }
    public void setLeaveTypeId(int leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getFromDate() {
        return fromDate;
    }
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public int getCreatedByUserId() {
        return createdByUserId;
    }
    public void setCreatedByUserId(int createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    // ---- MỚI: trạng thái ----
    public int getCurrentStatusId() {
        return currentStatusId;
    }
    public void setCurrentStatusId(int currentStatusId) {
        this.currentStatusId = currentStatusId;
    }

    public String getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    // ---- Dùng cho hiển thị ----
    public String getCreatedByName() {
        return createdByName;
    }
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getLeaveTypeName() {
        return leaveTypeName;
    }
    public void setLeaveTypeName(String leaveTypeName) {
        this.leaveTypeName = leaveTypeName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    /* ====== Helpers (tuỳ chọn) ======
       public boolean isInProgress() { return "INPROGRESS".equalsIgnoreCase(statusCode); }
       public boolean isApproved()   { return "APPROVED".equalsIgnoreCase(statusCode); }
       public boolean isRejected()   { return "REJECTED".equalsIgnoreCase(statusCode); }
    */
}
