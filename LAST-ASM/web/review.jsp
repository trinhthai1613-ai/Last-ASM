<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.leavemgmt.model.LeaveRequest" %>
<%
    LeaveRequest r = (LeaveRequest) request.getAttribute("req");
    if (r == null) {
%>
<p style="color:red">Request not found.</p>
<%  return; } %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Review</title>
</head>
<body style="font-family: Arial, sans-serif">
<h2>Review Request <%= (r.getRequestCode()==null?("#"+r.getRequestId()):r.getRequestCode()) %></h2>

<%
  String st = r.getStatusCode();
  String statusVi =
      "INPROGRESS".equalsIgnoreCase(st) ? "Đang xử lý" :
      "APPROVED".equalsIgnoreCase(st)  ? "Đã duyệt"   :
      "REJECTED".equalsIgnoreCase(st)  ? "Từ chối"    : st;

  String reason = r.getReason();
%>

<p>Type: <b><%= r.getTypeName() %></b> |
   Reason: <%= (reason==null || reason.trim().isEmpty()) ? "(không có)" : reason %></p>
<p>From: <%= r.getFromDate() %> → To: <%= r.getToDate() %></p>
<p>Created by: <%= r.getCreatedByName() %> |
   Status: <%= statusVi %></p>

<form method="post" action="<%=request.getContextPath()%>/app/request/review">
  <input type="hidden" name="id" value="<%= r.getRequestId() %>"/>

  <p>Note:</p>
  <textarea name="note" style="width:600px;height:90px"></textarea>
  <br/><br/>

  <button type="submit" name="decision" value="approve">Approve</button>
  <button type="submit" name="decision" value="reject">Reject</button>
  &nbsp; <a href="<%=request.getContextPath()%>/app/request/list?scope=team">Back</a>
</form>
</body>
</html>
