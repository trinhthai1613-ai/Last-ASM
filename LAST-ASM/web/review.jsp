<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.leavemgmt.model.LeaveRequest" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Review</title>
  <style>
    body { font-family: Arial, sans-serif; }
    textarea { width: 60%; min-height: 90px; }
    .btn { padding: 6px 10px; margin-right: 6px; }
  </style>
</head>
<body>

<%
  LeaveRequest r = (LeaveRequest) request.getAttribute("req");
  if (r == null) {
      out.println("<h3>Request not found.</h3>");
      return;
  }
  String reason = r.getReason();
  if (reason == null || reason.trim().isEmpty()) reason = "-";
%>

<h2>Review Request <%= r.getRequestCode() %></h2>

<p>Type: <%= r.getLeaveTypeName() %> | Reason: <%= reason %></p>
<p>From: <%= r.getFromDate() %> → To: <%= r.getToDate() %></p>
<p>Created by: <%= r.getCreatedByName() %> | Status: <%= r.getStatusCode() %></p>

<form method="post" action="<%=request.getContextPath()%>/app/request/review">
  <input type="hidden" name="id" value="<%= r.getRequestId() %>">

  <p>Note:</p>
  <textarea name="note"><%= request.getAttribute("note") != null ? request.getAttribute("note") : "" %></textarea>
  <br><br>

  <button class="btn" type="submit" name="decision" value="approve">Approve</button>
  <button class="btn" type="submit" name="decision" value="reject">Reject</button>
  <a class="btn" href="javascript:history.back()">Back</a>
</form>

</body>
</html>
