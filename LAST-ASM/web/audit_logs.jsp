<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.AuditLog, com.leavemgmt.model.User" %>
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><title>Audit Logs</title></head>
<body style="font-family:Arial, sans-serif">
<%
  User cur = (User) session.getAttribute("LOGIN_USER");
  // Phòng trường hợp ai đó vào trực tiếp qua URL
  if (cur == null || !cur.isTopLevel()) {
%>
  <p style="color:red">Bạn không có quyền xem trang này.</p>
  <p><a href="<%=request.getContextPath()%>/app/home">Back to Home</a></p>
</body>
</html>
<%  return; } %>

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
    <th>Status</th>         <%-- ← gộp old/new vào cột này --%>
    <th>Note</th>
  </tr>
<%
  List<AuditLog> logs = (List<AuditLog>) request.getAttribute("logs");
  if (logs != null) {
    for (AuditLog a : logs) {
      String oldSt = (a.getOldStatus()!=null?a.getOldStatus().trim():"");
      String newSt = (a.getNewStatus()!=null?a.getNewStatus().trim():"");
      String statusCell;
      if (!oldSt.isEmpty() && !newSt.isEmpty() && !oldSt.equalsIgnoreCase(newSt)) {
        statusCell = oldSt + " \u2192 " + newSt; // mũi tên →
      } else if (!newSt.isEmpty()) {
        statusCell = newSt;
      } else {
        statusCell = oldSt;
      }
%>
  <tr>
    <td><%= a.getOccurredAt() %></td>
    <td><%= a.getActorName() != null ? a.getActorName() : "" %></td>
    <td><%= a.getActionType() %></td>
    <td><%= a.getRequestCode() != null ? a.getRequestCode() : (a.getEntityKey()!=null ? a.getEntityKey() : "") %></td>
    <td><%= statusCell %></td>
    <td><%= a.getNote() != null ? a.getNote() : "" %></td>
  </tr>
<%  } } %>
</table>
</body>
</html>
