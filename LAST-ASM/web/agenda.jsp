<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.time.*, java.time.format.DateTimeFormatter, java.util.*" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Agenda</title>
<style>
  body { font-family: Arial, sans-serif }
  .toolbar { margin-bottom: 10px }
  table { border-collapse: collapse }
  th, td { border:1px solid #333; padding:6px 10px; text-align:center; }
  th.name, td.name { text-align:left; min-width:140px }
  .work    { background:#b6e7a1; }  /* xanh nhạt */
  .leave   { background:#ff6b6b; }  /* đỏ */
  .pending { background:#ffd54f; }  /* vàng */
  .weekend { color:#888; }
</style>
</head>
<body>

<%
  LocalDate start = (LocalDate) request.getAttribute("start");
  LocalDate end   = (LocalDate) request.getAttribute("end");
  @SuppressWarnings("unchecked")
  List<LocalDate> days = (List<LocalDate>) request.getAttribute("days");
  @SuppressWarnings("unchecked")
  Map<Integer,String> members = (Map<Integer,String>) request.getAttribute("members");
  @SuppressWarnings("unchecked")
  Map<Integer, Map<LocalDate,String>> grid =
          (Map<Integer, Map<LocalDate,String>>) request.getAttribute("grid");

  DateTimeFormatter dfmt = DateTimeFormatter.ofPattern("d/M");
%>

<h2>Agenda ( <%= start %> → <%= end %> )</h2>

<div class="toolbar">
  <form method="get" action="">
    From: <input type="date" name="start" value="<%= start %>">
    To: <input type="date" name="end" value="<%= end %>">
    <button type="submit">View</button>
    &nbsp;&nbsp;
    <a href="<%=request.getContextPath()%>/app/home">Home</a>
  </form>
</div>

<table>
  <tr>
    <th class="name">Nhân sự</th>
    <% for (LocalDate d : days) {
         boolean weekend = (d.getDayOfWeek()==DayOfWeek.SATURDAY || d.getDayOfWeek()==DayOfWeek.SUNDAY); %>
      <th class="<%= weekend ? "weekend" : "" %>"><%= d.format(dfmt) %></th>
    <% } %>
  </tr>

  <% if (members != null) {
       for (Map.Entry<Integer,String> row : members.entrySet()) {
         Integer uid = row.getKey();
         String name = row.getValue();
         Map<LocalDate,String> line = grid.get(uid);
  %>
    <tr>
      <td class="name"><%= name %></td>
      <% for (LocalDate d : days) {
           String cls = (line!=null) ? line.getOrDefault(d,"work") : "work"; %>
        <td class="<%= cls %>">&nbsp;</td>
      <% } %>
    </tr>
  <%   }
     } %>
</table>

<p style="margin-top:10px">
  <b>Chú thích:</b>
  <span class="work">&nbsp;&nbsp;&nbsp;</span> đi làm &nbsp;&nbsp;
  <span class="pending">&nbsp;&nbsp;&nbsp;</span> chờ duyệt &nbsp;&nbsp;
  <span class="leave">&nbsp;&nbsp;&nbsp;</span> nghỉ phép
</p>

</body>
</html>
