<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.LeaveRequest, com.leavemgmt.model.User" %>
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><title>Requests</title></head>
<body style="font-family:Arial, sans-serif">
<%
  User cur = (User) session.getAttribute("LOGIN_USER");
  boolean isTopLevel = (cur != null && cur.isTopLevel());
  String scope = (String) request.getAttribute("scope");
  String title = "team".equals(scope) ? "Team/Subtree" : "Mine";
  if (isTopLevel) title = "Team/Subtree"; // Top-level: không bao giờ hiển thị "Mine"
%>
<h2>Requests (<%= title %>)</h2>

<nav>
  <% if (!isTopLevel) { %>
    <a href="?scope=mine">Mine</a> |
  <% } %>
  <a href="?scope=team">Team/Subtree</a> |
  <a href="<%=request.getContextPath()%>/app/home">Home</a>
</nav>

<% String flash = (String) session.getAttribute("FLASH_MSG");
   if (flash != null) { %>
  <p style="color:green"><%= flash %></p>
<% session.removeAttribute("FLASH_MSG"); } %>

<table border="1" cellpadding="6" cellspacing="0">
  <tr>
    <th>Code</th><th>Type</th><th>Reason</th><th>From</th><th>To</th>
    <th>Created By</th><th>Status</th><th>Days (biz)</th><th>Action</th>
  </tr>
<%
  List<LeaveRequest> list = (List<LeaveRequest>) request.getAttribute("list");
  if (list != null) {
    for (LeaveRequest r : list) {
      String st = (r.getStatusName()==null?"":r.getStatusName());
      boolean isPending = st.equalsIgnoreCase("Inprogress") || st.equalsIgnoreCase("Pending")
                          || st.equalsIgnoreCase("Đang xử lý") || st.equalsIgnoreCase("Dang xu ly");
      boolean isOwn = r.getCreatedBy()!=null && cur!=null && r.getCreatedBy().equalsIgnoreCase(cur.getFullName());
%>
  <tr>
    <td><%=r.getRequestCode()%></td>
    <td><%=r.getTypeCode()%></td>
    <td><%= (r.getReasonCode()!=null?r.getReasonCode():"") %></td>
    <td><%=r.getFromDate()%></td>
    <td><%=r.getToDate()%></td>
    <td><%=r.getCreatedBy()%></td>
    <td><%=st%></td>
    <td><%= (r.getDaysBusiness()!=null?r.getDaysBusiness():"") %></td>
    <td>
      <% if (isPending && !isOwn) { %>
        <a href="<%=request.getContextPath()%>/app/request/review?id=<%=r.getRequestId()%>">Review</a>
      <% } else { %>-<% } %>
    </td>
  </tr>
<%  } } %>
</table>
</body>
</html>
