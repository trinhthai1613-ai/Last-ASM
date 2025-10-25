<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.leavemgmt.model.LeaveRequest" %>
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><title>Review</title></head>
<body style="font-family:Arial, sans-serif">
<%
  LeaveRequest r = (LeaveRequest) request.getAttribute("req");
%>
<h2>Review Request <%= (r!=null?r.getRequestCode():"") %></h2>
<% if (r!=null) { %>
  <p>Type: <%=r.getTypeCode()%> | Reason: <%= (r.getReasonCode()!=null?r.getReasonCode():"") %></p>
  <p>From: <%=r.getFromDate()%> → To: <%=r.getToDate()%> | Days(biz): <%= (r.getDaysBusiness()!=null?r.getDaysBusiness():"") %></p>
  <p>Created by: <%=r.getCreatedBy()%> | Status: <%=r.getStatusName()%></p>
  <form method="post">
    <input type="hidden" name="id" value="<%=r.getRequestId()%>"/>
    <label>Note:</label><br/>
    <textarea name="note" rows="3" cols="60"></textarea><br/>
    <button type="submit" name="action" value="approve">Approve</button>
    <button type="submit" name="action" value="reject">Reject</button>
    <a href="<%=request.getContextPath()%>/app/request/list?scope=team">Back</a>
  </form>
<% } %>
</body>
</html>
