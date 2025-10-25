<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.User, com.leavemgmt.model.LeaveType" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Create Leave Request</title>
<style>
  body { font-family: Arial, sans-serif }
  .row { margin: 8px 0 }
  select, input[type=date], textarea { font-size:14px }
  textarea { width:560px; height:100px }
  .btn { display:inline-block; padding:6px 12px; border:1px solid #444; text-decoration:none; margin-right:8px }
  .btn:hover { background:#eee }
  .alert { color:#b00020; margin:8px 0; }

  /* Popup phủ trắng toàn trang khi tạo xong */
  .modal { position:fixed; inset:0; background:#fff; display:flex; align-items:center; justify-content:center; z-index:9999; }
  .modal-box { background:#fff; border:1px solid #ddd; border-radius:10px; min-width:420px; padding:16px 20px; box-shadow:0 8px 30px rgba(0,0,0,.25) }
  .modal-title { margin:0 0 8px; font-size:18px }
  .modal-actions { margin-top:14px }
  .badge { color:#0a7a2a; font-weight:bold }
  .hidden { display:none }
  .no-scroll { overflow:hidden; }
</style>
</head>
<body>
<%
  User u = (User) session.getAttribute("LOGIN_USER");
  if (u == null) { response.sendRedirect(request.getContextPath()+"/login"); return; }

  @SuppressWarnings("unchecked")
  List<LeaveType> types = (List<LeaveType>) request.getAttribute("types"); // nạp từ servlet

  String createdId = request.getParameter("createdId");
  boolean justCreated = (createdId != null && !createdId.isEmpty());

  String flash = (String) session.getAttribute("FLASH_MSG");
  if (flash != null) session.removeAttribute("FLASH_MSG");
%>

<% if (justCreated) { %>
<script>document.addEventListener('DOMContentLoaded',()=>document.body.classList.add('no-scroll'));</script>
<% } %>

<h2>Create Leave Request</h2>

<%-- Thông báo lỗi khi tạo thất bại --%>
<% if (flash != null) { %>
  <div class="alert"><%= flash %></div>
<% } %>

<form method="post" action="<%=request.getContextPath()%>/app/request/create">
  <div class="row">
    <label>Leave Type:&nbsp;</label>
    <select name="typeId" id="typeId" required>
      <option value="">-- Select type --</option>
      <% if (types != null) {
           for (LeaveType t : types) { %>
        <option value="<%= t.getLeaveTypeId() %>" data-code="<%= t.getTypeCode() %>">
          <%= t.getTypeCode() %> - <%= t.getTypeName() %>
        </option>
      <% } } %>
    </select>
  </div>

  <div class="row" id="reasonRow" style="display:none">
    <label>Reason:</label><br/>
    <textarea name="reason" id="reason"></textarea>
  </div>

  <div class="row">
    <label>From:&nbsp;</label>
    <input type="date" name="fromDate" id="fromDate" required>
    &nbsp;&nbsp;
    <label>To:&nbsp;</label>
    <input type="date" name="toDate" id="toDate" required>
  </div>

  <div class="row">
    <button class="btn" type="submit">Send</button>
    <a class="btn" href="<%=request.getContextPath()%>/app/home">Back</a>
  </div>
</form>

<!-- POPUP THÀNH CÔNG: chỉ hiển thị sau khi redirect kèm ?createdId=... -->
<div id="successModal" class="modal <%= justCreated ? "" : "hidden" %>" aria-modal="true" role="dialog">
  <div class="modal-box">
    <h3 class="modal-title">Tạo đơn thành công</h3>
    <p>Request ID: <span class="badge">#<%= createdId %></span></p>
    <div class="modal-actions">
      <a class="btn" href="<%=request.getContextPath()%>/app/request/create">Create another</a>
      <a class="btn" href="<%=request.getContextPath()%>/app/request/list?scope=mine">My Requests</a>
      <a class="btn" href="<%=request.getContextPath()%>/app/home">Home</a>
    </div>
  </div>
</div>

<script>
  // Không cho chọn ngày đã qua
  const today = new Date(); today.setHours(0,0,0,0);
  const fmt = d => d.toISOString().slice(0,10);
  const fromEl = document.getElementById('fromDate');
  const toEl   = document.getElementById('toDate');
  fromEl.min = fmt(today); toEl.min = fmt(today);
  fromEl.addEventListener('change', () => {
    if (toEl.value < fromEl.value) toEl.value = fromEl.value;
    toEl.min = fromEl.value;
  });

  // Hiện ô Reason khi chọn OTHER
  const typeEl = document.getElementById('typeId');
  const reasonRow = document.getElementById('reasonRow');
  function refreshReasonUI() {
    const opt = typeEl.selectedOptions[0];
    const code = opt ? (opt.getAttribute('data-code')||'') : '';
    reasonRow.style.display = (code === 'OTHER') ? '' : 'none';
  }
  typeEl.addEventListener('change', refreshReasonUI);
  refreshReasonUI();
</script>
</body>
</html>
