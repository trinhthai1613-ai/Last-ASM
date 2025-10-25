<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.leavemgmt.model.User" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Login</title>
  <!-- Avoid caching this page so Back reflects current session state -->
  <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate"/>
  <meta http-equiv="Pragma" content="no-cache"/>
  <meta http-equiv="Expires" content="0"/>
</head>
<body style="font-family:Arial, sans-serif">
    <h2>Login</h2>
    <form method="post" action="login">
        <div>
            <label>Username:</label>
            <input type="text" name="username" required/>
        </div>
        <div>
            <label>Password:</label>
            <input type="password" name="password" required/>
        </div>
        <button type="submit">Sign in</button>
    </form>
    <p style="color:red"><%= (request.getAttribute("ERROR")!=null?request.getAttribute("ERROR"):"") %></p>
</body>
</html>
