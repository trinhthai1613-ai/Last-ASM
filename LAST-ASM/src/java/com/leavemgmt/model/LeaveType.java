package com.leavemgmt.model;

public class LeaveType {
    private int leaveTypeId;
    private String typeCode;
    private String typeName;

    public LeaveType() {}
    public LeaveType(int leaveTypeId, String typeCode, String typeName){
        this.leaveTypeId = leaveTypeId; this.typeCode = typeCode; this.typeName = typeName;
    }

    public int getLeaveTypeId(){ return leaveTypeId; }
    public void setLeaveTypeId(int leaveTypeId){ this.leaveTypeId = leaveTypeId; }

    public String getTypeCode(){ return typeCode; }
    public void setTypeCode(String typeCode){ this.typeCode = typeCode; }

    public String getTypeName(){ return typeName; }
    public void setTypeName(String typeName){ this.typeName = typeName; }

    @Override public String toString(){ return (typeCode!=null?typeCode:"") + (typeName!=null?" - "+typeName:""); }
}
