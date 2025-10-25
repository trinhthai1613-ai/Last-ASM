<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.leavemgmt.model.User" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Create Leave Request</title>
  <style>
    body { font-family: Arial, sans-serif }
    .row { margin: 8px 0 }
    select, input[type=date], textarea { font-size: 14px }
    textarea { width: 560px; height: 100px }
    .btn { display:inline-block; padding:6px 12px; border:1px solid #444; text-decoration:none; margin-right:8px }
    .btn:hover { background:#eee }

    /* Modal */
    .modal { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index: 9999; }
    .modal-box { background:#fff; border-radius:8px; min-width:420px; padding:16px 20px; box-shadow:0 8px 30px rgba(0,0,0,.25) }
    .modal-title { margin:0 0 8px; font-size:18px }
    .modal-actions { margin-top:14px }
    .badge { color:#0a7a2a; font-weight:bold }
    .hidden { display:none }
  </style>
</head>
<body>
<%
  User u = (User) session.getAttribute("LOGIN_USER");
  if (u == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }

  String createdId = request.getParameter("createdId");
  boolean justCreated = (createdId != null && !createdId.isEmpty());
%>

<h2>Create Leave Request</h2>

<nav>
  <a class="btn" href="<%=request.getContextPath()%>/app/home">Back</a>
</nav>

<!-- POPUP THÀNH CÔNG -->
<div id="successModal" class="modal <%= justCreated ? "" : "hidden" %>">
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

<!-- FORM: ẨN khi vừa tạo xong -->
<div id="formBlock" class="<%= justCreated ? "hidden" : "" %>">
  <form method="post" action="">
    <div class="row">
      <label>Leave Type:&nbsp;</label>
      <select name="typeId" id="typeId" required>
        <option value="">-- Select type --</option>
        <option value="1" data-need-reason="false">ANNUAL - Nghỉ năm</option>
        <option value="2" data-need-reason="true"  data-reason-opt="MARRIAGE,TRAVEL">MARRIAGE - Nghỉ cưới</option>
        <option value="3" data-need-reason="false">SICK - Nghỉ ốm</option>
        <option value="4" data-need-reason="false">UNPAID - Nghỉ không lương</option>
        <option value="99" data-need-reason="true">OTHER - Khác</option> <!-- OTHER dưới cùng -->
      </select>
    </div>

    <div class="row" id="reasonOptRow" style="display:none">
      <label>Reason option:&nbsp;</label>
      <select name="reasonOpt" id="reasonOpt"></select>
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
</div>

<script>
  // Ngăn chọn ngày quá khứ
  const today = new Date(); today.setHours(0,0,0,0);
  const fmt = d => d.toISOString().slice(0,10);
  const fromEl = document.getElementById('fromDate');
  const toEl   = document.getElementById('toDate');
  fromEl.min = fmt(today);
  toEl.min   = fmt(today);
  fromEl.addEventListener('change', () => { if (toEl.value < fromEl.value) toEl.value = fromEl.value; toEl.min = fromEl.value; });

  // Show/Hide reason & reason options theo loại đơn
  const typeEl   = document.getElementById('typeId');
  const reasonRow = document.getElementById('reasonRow');
  const reasonOptRow = document.getElementById('reasonOptRow');
  const reasonOptEl  = document.getElementById('reasonOpt');

  function refreshReasonUI() {
    const opt = typeEl.selectedOptions[0];
    if (!opt) return;
    const needReason = opt.getAttribute('data-need-reason') === 'true';
    const optList = (opt.getAttribute('data-reason-opt') || '').trim();

    reasonRow.style.display   = needReason ? '' : 'none';
    reasonOptRow.style.display = (optList ? '' : 'none');

    reasonOptEl.innerHTML = '';
    if (optList) {
      const items = optList.split(',');
      items.forEach((code) => {
        const o = document.createElement('option');
        o.value = code.trim();
        o.textContent = code.trim();
        reasonOptEl.appendChild(o);
      });
    }
  }
  typeEl.addEventListener('change', refreshReasonUI);
  refreshReasonUI();
</script>
</body>
</html>
