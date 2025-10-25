<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Agenda by Division</title>
</head>
<body style="font-family:Arial, sans-serif">
<h2>Agenda (theo phòng ban)</h2>
<p style="color:red;"><%= (request.getAttribute("ERROR")!=null?request.getAttribute("ERROR"):"") %></p>

<form method="get" action="">
  <label>Division:</label>
  <select name="divisionId" required>
    <option value="">-- Chọn phòng ban --</option>
    <%
      List<String[]> divisions = (List<String[]>) request.getAttribute("divisions");
      String selDiv = (String) request.getAttribute("divisionId");
      if (divisions != null) {
        for (String[] d : divisions) { // [0]=ID, [1]=Name
    %>
      <option value="<%=d[0]%>" <%= (selDiv!=null && selDiv.equals(d[0]) ? "selected" : "") %>><%=d[1]%></option>
    <%
        }
      }
    %>
  </select>

  <label style="margin-left:12px;">From:</label>
  <input type="date" name="fromDate" value="<%= (request.getAttribute("fromDate")!=null?request.getAttribute("fromDate"):"") %>" required>

  <label style="margin-left:12px;">To:</label>
  <input type="date" name="toDate" value="<%= (request.getAttribute("toDate")!=null?request.getAttribute("toDate"):"") %>" required>

  <button type="submit" style="margin-left:12px;">View</button>
  <a href="<%=request.getContextPath()%>/app/home" style="margin-left:12px;">Back</a>
</form>

<hr/>

<%
  List<String[]> rows = (List<String[]>) request.getAttribute("rows");
  if (rows != null) {
%>
  <table border="1" cellpadding="6" cellspacing="0">
    <tr>
      <th>Full Name</th>
      <th>Working Date</th>
      <th>Attendance</th>
    </tr>
    <%
      for (String[] r : rows) { // r[0]=FullName, r[1]=WorkingDate, r[2]=Attendance
    %>
      <tr>
        <td><%=r[0]%></td>
        <td><%=r[1]%></td>
        <td><%=r[2]%></td>
      </tr>
    <%
      }
    %>
  </table>
<%
  } else {
%>
  <p><i>Chọn Division và khoảng ngày rồi bấm View.</i></p>
<% } %>

</body>
</html>
