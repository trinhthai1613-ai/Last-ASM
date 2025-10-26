<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.Duration" %>
<%@ page import="java.time.Instant" %>
<%@ page import="com.leavemgmt.model.LeaveRequest" %>
<%@ page import="com.leavemgmt.model.User" %>
<%@ page import="com.leavemgmt.dao.RequestDAO" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Danh sách yêu cầu nghỉ phép</title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/style.css"/>
</head>
<body>
<%
  User cur = (User) session.getAttribute("LOGIN_USER");
  if (cur == null) { response.sendRedirect(request.getContextPath()+"/login"); return; }

  String scope = (String) request.getAttribute("scope");
  if (scope == null) scope = "mine";

  @SuppressWarnings("unchecked")
  List<LeaveRequest> list = (List<LeaveRequest>) request.getAttribute("list");
  if (list == null) list = java.util.Collections.emptyList();

  boolean teamScope = "team".equalsIgnoreCase(scope);
  String title = teamScope ? "Yêu cầu của đội / cây" : "Yêu cầu của tôi";
  boolean showActionsColumn = teamScope;
%>

<main class="container">
  <section class="pixel-card">
    <h1 class="pixel-heading"><%= title %></h1>

    <nav class="pixel-nav">
      <a href="<%=request.getContextPath()%>/app/home">Bảng điều khiển</a>
      <a href="<%=request.getContextPath()%>/app/request/create">Tạo yêu cầu mới</a>
    </nav>

    <%
      String flash = (String) session.getAttribute("FLASH_MSG");
      if (flash != null && !flash.isEmpty()) {
    %>
      <div class="pixel-alert success"><%= flash %></div>
    <%
        session.removeAttribute("FLASH_MSG");
      }
    %>

    <div style="overflow-x: auto;">
      <table class="pixel-table">
        <thead>
          <tr>
            <th>Mã</th>
            <th>Loại</th>
            <th>Lý do</th>
            <th>Từ ngày</th>
            <th>Đến ngày</th>
            <th>Người tạo</th>
            <th>Trạng thái</th>
            <th>Số ngày</th>
            <% if (showActionsColumn) { %>
              <th>Thao tác</th>
            <% } %>
          </tr>
        </thead>
        <tbody>
        <% if (list.isEmpty()) { %>
          <tr>
            <td colspan="<%= showActionsColumn ? 9 : 8 %>" style="text-align:center; padding: 2rem; color: var(--text-muted);">
              Chưa có yêu cầu nào.
            </td>
          </tr>
        <% } %>
        <% for (LeaveRequest r : list) {
             String code = (r.getStatusCode()==null? "" : r.getStatusCode().trim().toUpperCase());
             String statusVi;
             String statusClass = "pending";
             switch (code) {
               case "INPROGRESS": statusVi = "Đang xử lý"; statusClass = "pending"; break;
               case "APPROVED"  : statusVi = "Đã duyệt";   statusClass = "approved"; break;
               case "REJECTED"  : statusVi = "Từ chối";    statusClass = "rejected"; break;
               default          : statusVi = code;           statusClass = "pending";
             }
             boolean isPending = "INPROGRESS".equalsIgnoreCase(code);
             boolean isOwn = r.getCreatedByUserId() == cur.getUserId();
             java.sql.Timestamp createdAt = r.getCreatedAt();
             boolean withinOwnerWindow = false;
             if (createdAt != null) {
               Duration elapsed = Duration.between(createdAt.toInstant(), Instant.now());
               if (elapsed.isNegative()) elapsed = Duration.ZERO;
               withinOwnerWindow = elapsed.compareTo(Duration.ofMinutes(RequestDAO.OWNER_EDIT_WINDOW_MINUTES)) <= 0;
             }
             boolean showEdit = isOwn && isPending && withinOwnerWindow;
             boolean canReview = teamScope && (cur.isTopLevel() || !isOwn);
             boolean reviewableStatus = "INPROGRESS".equalsIgnoreCase(code) || "APPROVED".equalsIgnoreCase(code) || "REJECTED".equalsIgnoreCase(code);
             boolean showReview = canReview && reviewableStatus;
             String displayCode = r.getRequestCode()==null ? ("LR"+r.getRequestId()) : r.getRequestCode();
             String scopeParam = teamScope ? "team" : "mine";
        %>
          <tr>
            <td>
              <% if (showEdit && !showActionsColumn) { %>
                <a class="pixel-link" href="<%=request.getContextPath()%>/app/request/edit?id=<%=r.getRequestId()%>&scope=mine"><%= displayCode %></a>
              <% } else { %>
                <%= displayCode %>
              <% } %>
            </td>
            <td><%= r.getTypeName()==null ? r.getLeaveTypeId() : r.getTypeName() %></td>
            <td><%= r.getReason()==null ? "" : r.getReason() %></td>
            <td><%= r.getFromDate() %></td>
            <td><%= r.getToDate() %></td>
            <td><%= r.getCreatedByName()==null ? r.getCreatedByUserId() : r.getCreatedByName() %></td>
            <td><span class="pixel-status <%= statusClass %>"><%= statusVi %></span></td>
            <td><%= r.getBizDays()==null ? "" : r.getBizDays() %></td>
            <% if (showActionsColumn) { %>
              <td>
                <div class="pixel-actions">
                  <% boolean hasAction = false; %>
                  <% if (showEdit) { hasAction = true; %>
                    <a href="<%=request.getContextPath()%>/app/request/edit?id=<%=r.getRequestId()%>&scope=<%=scopeParam%>">Chỉnh sửa</a>
                  <% } %>
                  <% if (showReview) { %>
                    <% if (hasAction) { %><span>|</span><% } %>
                    <% hasAction = true; %>
                    <a href="<%=request.getContextPath()%>/app/request/review?id=<%=r.getRequestId()%>">Xét duyệt</a>
                  <% } %>
                  <% if (!hasAction) { %>
                    <span style="color: var(--text-muted);">-</span>
                  <% } %>
                </div>
              </td>
            <% } %>
          </tr>
        <% } %>
        </tbody>
      </table>
    </div>
  </section>
</main>
</body>
</html>
