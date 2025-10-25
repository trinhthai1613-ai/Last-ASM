package com.leavemgmt.model;

import java.sql.Date;

public class LeaveRequest {
    private int requestId;
    private String requestCode;
    private String typeCode;
    private String reasonCode;
    private Date fromDate;
    private Date toDate;
    private String createdBy;
    private String statusName;
    private Integer daysBusiness;
    private String processedBy;

    public int getRequestId(){ return requestId; }
    public void setRequestId(int requestId){ this.requestId = requestId; }

    public String getRequestCode(){ return requestCode; }
    public void setRequestCode(String requestCode){ this.requestCode = requestCode; }

    public String getTypeCode(){ return typeCode; }
    public void setTypeCode(String typeCode){ this.typeCode = typeCode; }

    public String getReasonCode(){ return reasonCode; }
    public void setReasonCode(String reasonCode){ this.reasonCode = reasonCode; }

    public Date getFromDate(){ return fromDate; }
    public void setFromDate(Date fromDate){ this.fromDate = fromDate; }

    public Date getToDate(){ return toDate; }
    public void setToDate(Date toDate){ this.toDate = toDate; }

    public String getCreatedBy(){ return createdBy; }
    public void setCreatedBy(String createdBy){ this.createdBy = createdBy; }

    public String getStatusName(){ return statusName; }
    public void setStatusName(String statusName){ this.statusName = statusName; }

    public Integer getDaysBusiness(){ return daysBusiness; }
    public void setDaysBusiness(Integer daysBusiness){ this.daysBusiness = daysBusiness; }

    public String getProcessedBy(){ return processedBy; }
    public void setProcessedBy(String processedBy){ this.processedBy = processedBy; }
}
