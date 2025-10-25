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
  if (cur == null) { response.sendRedirect(request.getContextPath()+"/login"); return; }

  boolean isTopLevel = cur.isTopLevel();
  boolean isLeaf = cur.isLeaf();

  String scope = (String) request.getAttribute("scope"); // "mine" | "team"
  if (scope == null) scope = "mine";
  String title = "team".equalsIgnoreCase(scope) ? "Team/Subtree" : "Mine";
  if (isTopLevel) title = "Team/Subtree";

  // CHỈ manager (không phải leaf) đang xem scope=team mới thấy cột Action
  boolean showAction = "team".equalsIgnoreCase(scope) && !isLeaf;
%>

<h2>Requests (<%= title %>)</h2>

<nav>
  <%-- Ẩn link Mine khi đang ở mine; top-level không có mine --%>
  <% if (!isTopLevel && !"mine".equalsIgnoreCase(scope)) { %>
    <a href="?scope=mine">Mine</a> |
  <% } %>
  <%-- Bỏ hẳn Team/Subtree trên UI cho leaf như yêu cầu trước đây --%>
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
    <% if (showAction) { %><th>Action</th><% } %>
  </tr>
<%
  List<LeaveRequest> list = (List<LeaveRequest>) request.getAttribute("list");
  if (list != null) {
    for (LeaveRequest r : list) {
      String st = (r.getStatusName()==null ? "" : r.getStatusName().trim().toLowerCase());
      boolean isPending = st.equals("inprogress") || st.equals("pending")
                       || st.equals("đang xử lý") || st.equals("dang xu ly");
      boolean isOwn = r.getCreatedBy()!=null && r.getCreatedBy().equalsIgnoreCase(cur.getFullName());
%>
  <tr>
    <td><%= r.getRequestCode() %></td>
    <td><%= r.getTypeCode() %></td>
    <td><%= (r.getReasonCode()!=null ? r.getReasonCode() : "") %></td>
    <td><%= r.getFromDate() %></td>
    <td><%= r.getToDate() %></td>
    <td><%= r.getCreatedBy() %></td>
    <td><%= (r.getStatusName()!=null ? r.getStatusName() : "") %></td>
    <td><%= (r.getDaysBusiness()!=null ? r.getDaysBusiness() : "") %></td>

    <% if (showAction) { %>
      <td>
        <% if (isPending && !isOwn) { %>
          <a href="<%=request.getContextPath()%>/app/request/review?id=<%= r.getRequestId() %>">Review</a>
        <% } else { %> - <% } %>
      </td>
    <% } %>
  </tr>
<%  } } %>
</table>
</body>
</html>
