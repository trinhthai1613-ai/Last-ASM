package com.leavemgmt.model;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * POJO dùng cho list / review.
 * Bổ sung: typeName, createdByName, statusCode, currentStatusId, bizDays.
 */
public class LeaveRequest {
    private int requestId;
    private String requestCode;

    private int leaveTypeId;
    private String typeName;          // <-- để JSP show tên loại nghỉ

    private String reason;

    private Date fromDate;
    private Date toDate;

    private int createdByUserId;
    private String createdByName;     // <-- hiển thị người tạo

    private int currentStatusId;      // id trạng thái hiện tại (FK)
    private String statusCode;        // INPROGRESS / APPROVED / REJECTED

    private Timestamp createdAt;

    // Có thể lấy từ DB hoặc tự tính; nếu null thì getter sẽ tự tính.
    private Integer bizDays;          // số ngày làm việc (không tính T7, CN)

    // ---- Getters / Setters ----
    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }

    public String getRequestCode() { return requestCode; }
    public void setRequestCode(String requestCode) { this.requestCode = requestCode; }

    public int getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(int leaveTypeId) { this.leaveTypeId = leaveTypeId; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }

    public Date getToDate() { return toDate; }
    public void setToDate(Date toDate) { this.toDate = toDate; }

    public int getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(int createdByUserId) { this.createdByUserId = createdByUserId; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public int getCurrentStatusId() { return currentStatusId; }
    public void setCurrentStatusId(int currentStatusId) { this.currentStatusId = currentStatusId; }

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /**
     * Số ngày làm việc. Nếu DB không trả ra thì sẽ tự tính từ fromDate..toDate,
     * bỏ qua T7 (SAT) và CN (SUN).
     */
    public Integer getBizDays() {
        if (bizDays != null) return bizDays;

        if (fromDate == null || toDate == null) return null;
        LocalDate s = fromDate.toLocalDate();
        LocalDate e = toDate.toLocalDate();

        int count = 0;
        for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) count++;
        }
        return count;
    }
    public void setBizDays(Integer bizDays) { this.bizDays = bizDays; }
}
