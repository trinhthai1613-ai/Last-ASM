<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.leavemgmt.model.User" %>
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><title>Home</title></head>
<body style="font-family:Arial, sans-serif">
<%
  User u = (User) session.getAttribute("LOGIN_USER");
  boolean isTopLevel = (u != null && u.isTopLevel());
  boolean isLeaf     = (u != null && u.isLeaf());
%>

<h2>Welcome, <%= u.getFullName() %> (<%= u.getUsername() %>) — Division: <%= u.getDivisionName() %></h2>

<nav>
  <% if (!isTopLevel) { %>
    <a href="<%=request.getContextPath()%>/app/request/create">Create Request</a> |
    <a href="<%=request.getContextPath()%>/app/request/list?scope=mine">My Requests</a> |
  <% } %>

  <%-- Team/Subtree: ẨN HẲN đối với leaf --%>
  <% if (!isLeaf) { %>
    <a href="<%=request.getContextPath()%>/app/request/list?scope=team">Team/Subtree</a> |
  <% } %>

  <% if (isTopLevel) { %>
    <a href="<%=request.getContextPath()%>/app/audit/logs">Audit Log</a> |
  <% } %>

  <% if (!isLeaf) { %>
    <a href="<%=request.getContextPath()%>/app/agenda">Agenda</a> |
  <% } %>

  <a href="<%=request.getContextPath()%>/logout">Logout</a>
</nav>

<%
  String flash = (String) session.getAttribute("FLASH_MSG");
  if (flash != null) { %>
  <p style="color:green;"><%= flash %></p>
<% session.removeAttribute("FLASH_MSG"); } %>
</body>
</html>
