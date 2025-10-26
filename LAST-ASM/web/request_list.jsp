<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.Duration" %>
<%@ page import="java.time.Instant" %>
<%@ page import="com.leavemgmt.model.LeaveRequest" %>
<%@ page import="com.leavemgmt.model.User" %>
<%@ page import="com.leavemgmt.dao.RequestDAO" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Requests</title>
  <style>
    table{ border-collapse: collapse; }
    th,td{ border:1px solid #999; padding:6px 10px; }
    th{ background:#f5f5f5; }
  </style>
</head>
<body style="font-family:Arial, sans-serif">

<%
  User cur = (User) session.getAttribute("LOGIN_USER");
  if (cur == null) { response.sendRedirect(request.getContextPath()+"/login"); return; }

  String scope = (String) request.getAttribute("scope"); // "mine" | "team"
  if (scope == null) scope = "mine";

  @SuppressWarnings("unchecked")
  List<LeaveRequest> list = (List<LeaveRequest>) request.getAttribute("list");
  if (list == null) list = java.util.Collections.emptyList();

  String title = "Requests";
  if ("team".equalsIgnoreCase(scope)) title = "Requests (Team/Subtree)";
  else title = "Requests (Mine)";
%>

<h2><%= title %></h2>
<p>
  <a href="<%=request.getContextPath()%>/app/home">Home</a>
</p>

<%
  String flash = (String) session.getAttribute("FLASH_MSG");
  if (flash != null) {
%>
  <p style="color:green"><%= flash %></p>
<%
    session.removeAttribute("FLASH_MSG");
  }
%>

<table>
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

      <th>Actions</th>
    </tr>
  </thead>
  <tbody>
  <% for (LeaveRequest r : list) {
       String code = (r.getStatusCode()==null? "" : r.getStatusCode().trim().toUpperCase());
       String statusVi;
       switch (code) {
         case "INPROGRESS": statusVi = "Đang xử lý"; break;
         case "APPROVED"  : statusVi = "Đã duyệt";   break;
         case "REJECTED"  : statusVi = "Từ chối";    break;
         default          : statusVi = code;
       }
       boolean isPending = "INPROGRESS".equalsIgnoreCase(code);
       boolean isOwn = r.getCreatedByUserId() == cur.getUserId();
       java.sql.Timestamp createdAt = r.getCreatedAt();
       boolean withinOwnerWindow = false;
       if (createdAt != null) {
         Duration elapsed = Duration.between(createdAt.toInstant(), Instant.now());
         if (elapsed.isNegative()) elapsed = Duration.ZERO;
         withinOwnerWindow = elapsed.compareTo(Duration.ofMinutes(RequestDAO.OWNER_EDIT_WINDOW_MINUTES)) <= 0;
       }
       boolean showEdit = isOwn && isPending && withinOwnerWindow;
       boolean canReview = "team".equalsIgnoreCase(scope) && (cur.isTopLevel() || !isOwn);
       boolean reviewableStatus = "INPROGRESS".equalsIgnoreCase(code) || "APPROVED".equalsIgnoreCase(code) || "REJECTED".equalsIgnoreCase(code);
       boolean showReview = canReview && reviewableStatus;
  %>
    <tr>
      <td><%= r.getRequestCode()==null ? ("LR"+r.getRequestId()) : r.getRequestCode() %></td>
      <td><%= r.getTypeName()==null ? r.getLeaveTypeId() : r.getTypeName() %></td>
      <td><%= r.getReason()==null ? "" : r.getReason() %></td>
      <td><%= r.getFromDate() %></td>
      <td><%= r.getToDate() %></td>
      <td><%= r.getCreatedByName()==null ? r.getCreatedByUserId() : r.getCreatedByName() %></td>
      <td><%= statusVi %></td>
      <td><%= r.getBizDays()==null ? "" : r.getBizDays() %></td>


      <td>
        <%
          String scopeParam = "team".equalsIgnoreCase(scope) ? "team" : "mine";
          boolean hasAction = false;
          if (showEdit) {
        %>
            <a href="<%=request.getContextPath()%>/app/request/edit?id=<%=r.getRequestId()%>&scope=<%=scopeParam%>">Edit</a>
        <%
            hasAction = true;
          }
          if (showReview) {
            if (hasAction) { out.print(" | "); }
        %>
            <a href="<%=request.getContextPath()%>/app/request/review?id=<%=r.getRequestId()%>">Review</a>
    
        <%
            hasAction = true;
          }
          if (!hasAction) {
            out.print("-");
          }
        %>
      </td>
    </tr>
  <% } %>
  </tbody>
</table>

</body>
</html>