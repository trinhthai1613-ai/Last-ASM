<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.leavemgmt.model.User" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Galactic Leave Console</title>
  <!-- Avoid caching this page so Back reflects current session state -->
  <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate"/>
  <meta http-equiv="Pragma" content="no-cache"/>
  <meta http-equiv="Expires" content="0"/>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/style.css"/>
</head>
<body>
  <main class="pixel-layout">
    <section class="pixel-card" style="max-width: 420px; width: 100%;">
      <h1 class="pixel-heading">Galactic Leave Console</h1>
      <p>Đăng nhập để điều khiển trung tâm nghỉ phép của bạn.</p>

      <form method="post" action="login" class="pixel-form">
        <div>
          <label for="username">Username</label>
          <input class="pixel-input" type="text" id="username" name="username" required autocomplete="username"/>
        </div>
        <div>
          <label for="password">Password</label>
          <input class="pixel-input" type="password" id="password" name="password" required autocomplete="current-password"/>
        </div>
        <button class="pixel-button" type="submit">Đăng nhập</button>
        <%
          String error = (String) request.getAttribute("ERROR");
          if (error != null && !error.isEmpty()) {
        %>
          <p class="pixel-error"><%= error %></p>
        <% } %>
      </form>
    </section>
  </main>
</body>
</html>
