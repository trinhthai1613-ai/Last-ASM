<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.leavemgmt.model.User" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Galactic Leave Console</title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/style.css"/>
</head>
<body>
<%
  User u = (User) session.getAttribute("LOGIN_USER");
  boolean isTopLevel = (u != null && u.isTopLevel());
  boolean isLeaf     = (u != null && u.isLeaf());
%>

  <main class="container">
    <section class="pixel-card">
      <h1 class="pixel-heading">Xin chào, <%= u.getFullName() %></h1>
      <div class="pixel-badge" style="margin-bottom: 2rem;">
        <span>Tài khoản</span>
        <strong><%= u.getUsername() %></strong>
        <span>Đơn vị</span>
        <strong><%= u.getDivisionName() %></strong>
      </div>

      <nav class="pixel-nav">
        <% if (!isTopLevel) { %>
          <a href="<%=request.getContextPath()%>/app/request/create">Tạo yêu cầu</a>
          <a href="<%=request.getContextPath()%>/app/request/list?scope=mine">Yêu cầu của tôi</a>
        <% } %>

        <% if (!isLeaf) { %>
          <a href="<%=request.getContextPath()%>/app/request/list?scope=team">Nhóm/Chuỗi</a>
        <% } %>

        <% if (isTopLevel) { %>
          <a href="<%=request.getContextPath()%>/app/audit/logs">Nhật ký kiểm toán</a>
          <a href="<%=request.getContextPath()%>/app/agenda">Agenda</a>
        <% } %>

        <a href="<%=request.getContextPath()%>/logout">Đăng xuất</a>
      </nav>

      <%
        String flash = (String) session.getAttribute("FLASH_MSG");
        if (flash != null && !flash.isEmpty()) {
      %>
        <p class="pixel-alert success"><%= flash %></p>
      <% session.removeAttribute("FLASH_MSG"); } %>
    </section>
  </main>
</body>
</html>
