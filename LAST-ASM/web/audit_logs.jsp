<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.AuditLog" %>
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><title>Audit Logs</title></head>
<body style="font-family:Arial, sans-serif">
<h2>Audit Logs</h2>
<nav>
  <a href="<%=request.getContextPath()%>/app/home">Home</a>
</nav>
<hr/>

<table border="1" cellpadding="6" cellspacing="0">
  <tr>
    <th>Time (UTC)</th>
    <th>Actor</th>
    <th>Action</th>
    <th>Request Code</th>
    <th>Old Status</th>
    <th>New Status</th>
    <th>Note</th>
  </tr>
<%
  List<AuditLog> logs = (List<AuditLog>) request.getAttribute("logs");
  if (logs != null) {
    for (AuditLog a : logs) {
%>
  <tr>
    <td><%= a.getOccurredAt() %></td>
    <td><%= a.getActorName() != null ? a.getActorName() : "" %></td>
    <td><%= a.getActionType() %></td>
    <td><%= a.getRequestCode() != null ? a.getRequestCode() : "" %></td>
    <td><%= a.getOldStatus() != null ? a.getOldStatus() : "" %></td>
    <td><%= a.getNewStatus() != null ? a.getNewStatus() : "" %></td>
    <td><%= a.getNote() != null ? a.getNote() : "" %></td>
  </tr>
<% } } %>
</table>
</body>
</html>
