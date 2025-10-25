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
  .work    { background:#b6e7a1; }  /* xanh */
  .leave   { background:#ff6b6b; }  /* đỏ */
  .holiday { background:#d9d9d9; }  /* xám (Chủ nhật) */
  .weekend-head { color:#666; }
  .pager a { margin:0 6px; text-decoration:none; }
  .pager span.disabled { color:#aaa; margin:0 6px; }
</style>
</head>
<body>

<%
  LocalDate start = (LocalDate) request.getAttribute("start");
  LocalDate end   = (LocalDate) request.getAttribute("end");
  int week        = (Integer) request.getAttribute("week");
  boolean hasPrev = (Boolean) request.getAttribute("hasPrev");
  boolean hasNext = (Boolean) request.getAttribute("hasNext");
  LocalDate pageStart = (LocalDate) request.getAttribute("pageStart");
  LocalDate pageEnd   = (LocalDate) request.getAttribute("pageEnd");

  @SuppressWarnings("unchecked")
  List<LocalDate> days = (List<LocalDate>) request.getAttribute("days");
  @SuppressWarnings("unchecked")
  Map<Integer,String> members = (Map<Integer,String>) request.getAttribute("members");
  @SuppressWarnings("unchecked")
  Map<Integer, Map<LocalDate,String>> grid =
          (Map<Integer, Map<LocalDate,String>>) request.getAttribute("grid");

  DateTimeFormatter dfmt = DateTimeFormatter.ofPattern("d/M");
%>

<h2>Agenda (Tuần <%= week + 1 %>) <%= pageStart %> → <%= pageEnd %></h2>

<div class="toolbar">
  <!-- Chọn khoảng thời gian gốc -->
  <form method="get" action="">
    From: <input type="date" name="start" value="<%= start %>">
    To:   <input type="date" name="end"   value="<%= end %>">
    <button type="submit">View</button>
    &nbsp;&nbsp; <a href="<%=request.getContextPath()%>/app/home">Home</a>
  </form>

  <!-- Điều hướng tuần -->
  <div class="pager" style="margin-top:8px">
    <% String base = request.getContextPath()+"/app/agenda?start="+start+"&end="+end+"&week="; %>
    <% if (hasPrev) { %>
      <a href="<%= base + (week - 1) %>">« Prev</a>
    <% } else { %>
      <span class="disabled">« Prev</span>
    <% } %>
    <% if (hasNext) { %>
      <a href="<%= base + (week + 1) %>">Next »</a>
    <% } else { %>
      <span class="disabled">Next »</span>
    <% } %>
  </div>
</div>

<table>
  <tr>
    <th class="name">Nhân sự</th>
    <% for (LocalDate d : days) {
         boolean sunday = (d.getDayOfWeek() == DayOfWeek.SUNDAY); %>
      <th class="<%= sunday ? "weekend-head" : "" %>"><%= d.format(dfmt) %></th>
    <% } %>
  </tr>

  <% if (members != null && !members.isEmpty()) {
       for (Map.Entry<Integer,String> row : members.entrySet()) {
         Integer uid = row.getKey();
         String name = row.getValue();
         Map<LocalDate,String> line = grid.get(uid);
  %>
    <tr>
      <td class="name"><%= name %></td>
      <% for (LocalDate d : days) {
           boolean sunday = (d.getDayOfWeek() == DayOfWeek.SUNDAY);
           String cls;
           if (sunday) {
               cls = "holiday"; // Chủ nhật luôn xám
           } else {
               cls = (line!=null) ? line.getOrDefault(d,"work") : "work";
               if (!"leave".equals(cls)) cls = "work"; // mọi thứ khác coi là xanh
           }
      %>
        <td class="<%= cls %>">&nbsp;</td>
      <% } %>
    </tr>
  <%   }
     } %>
</table>

</body>
</html>
