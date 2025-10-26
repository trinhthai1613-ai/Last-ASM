<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.time.*, java.time.format.DateTimeFormatter, java.util.*" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Lịch nghỉ phép</title>
<link rel="stylesheet" href="<%=request.getContextPath()%>/style.css"/>
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

<main class="container">
  <section class="pixel-card">
    <h1 class="pixel-heading">Lịch nghỉ phép — Tuần <%= week + 1 %></h1>
    <p>Khoảng thời gian: <strong><%= pageStart %></strong> → <strong><%= pageEnd %></strong></p>

    <div class="pixel-toolbar">
      <form method="get" action="" class="pixel-form pixel-form-inline" style="margin:0;">
        <div>
          <label for="start">Từ ngày</label>
          <input class="pixel-input" type="date" id="start" name="start" value="<%= start %>">
        </div>
        <div>
          <label for="end">Đến ngày</label>
          <input class="pixel-input" type="date" id="end" name="end" value="<%= end %>">
        </div>
        <div class="pixel-form-actions">
          <button class="pixel-button" type="submit">Xem lịch</button>
          <a class="pixel-button secondary" href="<%=request.getContextPath()%>/app/home">Bảng điều khiển</a>
        </div>
      </form>

      <div class="pixel-pager">
        <% String base = request.getContextPath()+"/app/agenda?start="+start+"&end="+end+"&week="; %>
        <% if (hasPrev) { %>
          <a href="<%= base + (week - 1) %>">« Tuần trước</a>
        <% } else { %>
          <span class="disabled">« Tuần trước</span>
        <% } %>
        <% if (hasNext) { %>
          <a href="<%= base + (week + 1) %>">Tuần sau »</a>
        <% } else { %>
          <span class="disabled">Tuần sau »</span>
        <% } %>
      </div>
    </div>

    <div style="overflow-x:auto;">
      <table class="pixel-table agenda">
        <tr>
          <th class="name">Nhân sự</th>
          <% for (LocalDate d : days) {
               boolean sunday = (d.getDayOfWeek() == DayOfWeek.SUNDAY); %>
            <th class="agenda-head <%= sunday ? "weekend" : "" %>"><%= d.format(dfmt) %></th>
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
                     cls = "holiday";
                 } else {
                     cls = (line!=null) ? line.getOrDefault(d,"work") : "work";
                     if (!"leave".equals(cls)) cls = "work";
                 }
            %>
              <td class="agenda-cell <%= cls %>">&nbsp;</td>
            <% } %>
          </tr>
        <%   }
           } else { %>
          <tr>
            <td colspan="<%= days.size() + 1 %>" style="text-align:center; padding:2rem; color: var(--text-muted);">
              Không có dữ liệu hiển thị.
            </td>
          </tr>
        <% } %>
      </table>
    </div>
  </section>
</main>
</body>
</html>
