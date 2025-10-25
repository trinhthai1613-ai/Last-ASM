package com.leavemgmt.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class User implements Serializable {
    private int userId;
    private String username;
    private String fullName;
    private String email;
    private int divisionId;
    private String divisionName;

    private Set<String> roleCodes = new HashSet<>();
    private Integer currentManagerId;   // null => top-level (không có cấp trên)
    private boolean hasSubordinates;    // true => là quản lý (có cấp dưới)

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getDivisionId() { return divisionId; }
    public void setDivisionId(int divisionId) { this.divisionId = divisionId; }
    public String getDivisionName() { return divisionName; }
    public void setDivisionName(String divisionName) { this.divisionName = divisionName; }
    public Set<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(Set<String> roleCodes) { this.roleCodes = roleCodes; }

    public Integer getCurrentManagerId() { return currentManagerId; }
    public void setCurrentManagerId(Integer currentManagerId) { this.currentManagerId = currentManagerId; }

    public boolean isTopLevel() { return currentManagerId == null; }
    public boolean isLeaf() { return !hasSubordinates; } // nhân viên cấp thấp nhất
    public boolean hasRole(String code) { return roleCodes != null && roleCodes.contains(code); }

    public boolean isHasSubordinates() { return hasSubordinates; }
    public void setHasSubordinates(boolean hasSubordinates) { this.hasSubordinates = hasSubordinates; }
}
