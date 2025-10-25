<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.LeaveRequest, com.leavemgmt.model.User" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Requests</title>
  <style>
    body{font-family:Arial, sans-serif}
    table{border-collapse:collapse; min-width:880px}
    th,td{border:1px solid #999; padding:6px 8px}
    th{background:#f2f2f2}
  </style>
</head>
<body>

<%
  User cur = (User) session.getAttribute("LOGIN_USER");
  String scope = (String) request.getAttribute("scope"); // "mine" | "team"
  @SuppressWarnings("unchecked")
  List<LeaveRequest> list = (List<LeaveRequest>) request.getAttribute("list");
%>

<h2>Requests <%= ("team".equalsIgnoreCase(scope) ? "(Team/Subtree)" : "(Mine)") %></h2>

<a href="<%=request.getContextPath()%>/home">Home</a>
<% if (!"mine".equalsIgnoreCase(scope)) { %> | 
   <a href="<%=request.getContextPath()%>/app/request/list?scope=mine">Mine</a>
<% } %>
<% if (!"team".equalsIgnoreCase(scope)) { %> | 
   <a href="<%=request.getContextPath()%>/app/request/list?scope=team">Team/Subtree</a>
<% } %>

<table style="margin-top:10px">
  <thead>
    <tr>
      <th>Code</th>
      <th>Type</th>
      <th>Reason</th>
      <th>From</th>
      <th>To</th>
      <th>Created By</th>
      <th>Status</th>
      <th>Days (biz)</th>
    </tr>
  </thead>
  <tbody>
  <%
    if (list != null) {
      for (LeaveRequest r : list) {
        String code = r.getStatusCode()==null ? "" : r.getStatusCode().toUpperCase();
        String statusVi;
        switch (code) {
          case "INPROGRESS": statusVi = "Đang xử lý"; break;
          case "APPROVED":  statusVi = "Đã duyệt";   break;
          case "REJECTED":  statusVi = "Từ chối";    break;
          default:          statusVi = code;         break;
        }
  %>
    <tr>
      <td><%= r.getRequestCode() %></td>
      <td><%= r.getTypeName() %></td>
      <td><%= r.getReason()==null ? "" : r.getReason() %></td>
      <td><%= r.getFromDate() %></td>
      <td><%= r.getToDate() %></td>
      <td><%= r.getCreatedByName()==null ? "" : r.getCreatedByName() %></td>
      <td><%= statusVi %></td>
      <td><%= r.getBizDays()==null ? "" : r.getBizDays() %></td>
    </tr>
  <%
      }
    }
  %>
  </tbody>
</table>

</body>
</html>
