<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.leavemgmt.model.LeaveRequest" %>
<%
    LeaveRequest r = (LeaveRequest) request.getAttribute("req");
    if (r == null) {
%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Không tìm thấy yêu cầu</title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/style.css"/>
</head>
<body>
  <main class="pixel-layout">
    <section class="pixel-card" style="max-width:420px;">
      <div class="pixel-alert error">Không tìm thấy yêu cầu.</div>
      <a class="pixel-link" href="<%=request.getContextPath()%>/app/request/list?scope=team">Quay về danh sách</a>
    </section>
  </main>
</body>
</html>
<%  return; } %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Xét duyệt yêu cầu</title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/style.css"/>
</head>
<body>
<%
  String st = r.getStatusCode();
  String statusClass = "pending";
  String statusVi =
      "INPROGRESS".equalsIgnoreCase(st) ? "Đang xử lý" :
      "APPROVED".equalsIgnoreCase(st)  ? "Đã duyệt"   :
      "REJECTED".equalsIgnoreCase(st)  ? "Từ chối"    : st;
  if ("APPROVED".equalsIgnoreCase(st)) {
    statusClass = "approved";
  } else if ("REJECTED".equalsIgnoreCase(st)) {
    statusClass = "rejected";
  }
  String reason = r.getReason();
%>

<main class="container">
  <section class="pixel-card">
    <h1 class="pixel-heading">Xét duyệt yêu cầu <%= (r.getRequestCode()==null?("#"+r.getRequestId()):r.getRequestCode()) %></h1>

    <div class="pixel-grid">
      <div>
        <h2 class="pixel-heading" style="font-size:1rem;">Thông tin chính</h2>
        <p>Loại phép: <strong><%= r.getTypeName() %></strong></p>
        <p>Lý do: <%= (reason==null || reason.trim().isEmpty()) ? "(không có)" : reason %></p>
        <p>Khoảng thời gian: <%= r.getFromDate() %> → <%= r.getToDate() %></p>
      </div>
      <div>
        <h2 class="pixel-heading" style="font-size:1rem;">Người tạo</h2>
        <p><strong><%= r.getCreatedByName() %></strong></p>
        <p>Trạng thái hiện tại:</p>
        <p><span class="pixel-status <%= statusClass %>"><%= statusVi %></span></p>
      </div>
    </div>

    <form method="post" action="<%=request.getContextPath()%>/app/request/review" class="pixel-form" style="margin-top:2rem;">
      <input type="hidden" name="id" value="<%= r.getRequestId() %>"/>
      <div class="pixel-form-actions">
        <button class="pixel-button" type="submit" name="decision" value="approve">Duyệt</button>
        <button class="pixel-button secondary" type="submit" name="decision" value="reject">Từ chối</button>
        <a class="pixel-button secondary" href="<%=request.getContextPath()%>/app/request/list?scope=team" style="text-align:center;">Quay lại</a>
      </div>
    </form>
  </section>
</main>
</body>
</html>
