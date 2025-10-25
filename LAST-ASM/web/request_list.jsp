<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.LeaveRequest, com.leavemgmt.model.User" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Requests</title>
</head>
<body style="font-family:Arial, sans-serif">
<%
  User cur = (User) session.getAttribute("LOGIN_USER");
  boolean isTopLevel = (cur != null && cur.isTopLevel());
  String scope = (String) request.getAttribute("scope");
  String title = "team".equalsIgnoreCase(scope) ? "Team/Subtree" : "Mine";
  if (isTopLevel) title = "Team/Subtree";
%>

<h2>Requests (<%= title %>)</h2>

<nav>
  <% if (!isTopLevel) { %>
    <a href="?scope=mine">Mine</a> |
  <% } %>
  <!-- BỎ HẲN Team/Subtree -->
  <a href="<%=request.getContextPath()%>/app/home">Home</a>
</nav>

<%
  String flash = (String) session.getAttribute("FLASH_MSG");
  if (flash != null) { %>
    <p style="color:green;"><%= flash %></p>
<%  session.removeAttribute("FLASH_MSG"); } %>

<table border="1" cellpadding="6" cellspacing="0">
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
<%
  List<LeaveRequest> list = (List<LeaveRequest>) request.getAttribute("list");
  if (list != null) {
    for (LeaveRequest r : list) {
%>
  <tr>
    <td><%= r.getRequestCode() %></td>
    <td><%= r.getTypeCode() %></td>
    <td><%= (r.getReasonCode() != null ? r.getReasonCode() : "") %></td>
    <td><%= r.getFromDate() %></td>
    <td><%= r.getToDate() %></td>
    <td><%= r.getCreatedBy() %></td>
    <td><%= (r.getStatusName() != null ? r.getStatusName() : "") %></td>
    <td><%= (r.getDaysBusiness() != null ? r.getDaysBusiness() : "") %></td>
  </tr>
<%  } } %>
</table>
</body>
</html>
