<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.LeaveType" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Create Leave Request</title>
  <style>
    body{font-family:Arial, sans-serif}
    label{display:inline-block;min-width:90px;margin:8px 0}
    input, select, textarea{padding:6px}
    textarea{width:420px;height:90px}
    .hidden{display:none}

    /* Modal */
    .backdrop{
      position:fixed; inset:0; background:#0008; z-index:9998;
    }
    .modal{
      position:fixed; z-index:9999; inset:0; display:flex;
      align-items:center; justify-content:center;
    }
    .modal > div{
      background:#fff; padding:16px 18px; border-radius:8px; min-width:360px;
      box-shadow:0 8px 28px rgba(0,0,0,.25);
    }
    .modal h3{margin:0 0 8px 0}
    .actions{margin-top:12px; display:flex; gap:8px}
    .btn{padding:6px 12px; border:1px solid #444; background:#fafafa; cursor:pointer}
  </style>
</head>
<body>

<h2>Create Leave Request</h2>

<%
  // 'types' do servlet setAttribute đưa xuống: List<LeaveType>
  @SuppressWarnings("unchecked")
  List<LeaveType> types = (List<LeaveType>) request.getAttribute("types");
  if (types == null) types = new ArrayList<>();

  String createdId = request.getParameter("createdId"); // để bật modal
%>

<form id="frmCreate" method="post" action="<%=request.getContextPath()%>/app/request/create">
  <div>
    <label>Leave Type:</label>
    <select id="typeId" name="typeId" required>
      <option value="">-- Select type --</option>
      <%
        for (LeaveType t : types) {
          // Nếu model có TypeCode thì in ra luôn data-code để JS nhận biết “OTHER”
          String code = "";
          try { 
            java.lang.reflect.Method m = t.getClass().getMethod("getTypeCode");
            Object v = m.invoke(t);
            code = (v==null) ? "" : v.toString();
          } catch (Exception ignore) {}
      %>
        <option value="<%=t.getLeaveTypeId()%>" data-code="<%=code%>">
          <%= t.getTypeName() %>
        </option>
      <% } %>
    </select>
  </div>

  <div id="reasonGroup" class="hidden">
    <label>Reason:</label>
    <textarea id="reason" name="reason" placeholder="Nhập lý do cụ thể..."></textarea>
  </div>

  <div>
    <label>From:</label>
    <input type="date" id="from" name="from" required>
    <label style="min-width:auto;margin-left:10px">To:</label>
    <input type="date" id="to" name="to" required>
  </div>

  <div style="margin-top:10px">
    <button class="btn" type="submit" id="btnSend">Send</button>
    <a class="btn" href="<%=request.getContextPath()%>/home">Back</a>
  </div>
</form>

<% if (createdId != null && !createdId.isBlank()) { %>
  <!-- Modal success -->
  <div class="backdrop"></div>
  <div class="modal">
    <div>
      <h3>Tạo đơn thành công!</h3>
      <p>Request ID: <strong><%= createdId %></strong></p>
      <div class="actions">
        <button class="btn" onclick="location.href='<%=request.getContextPath()%>/app/request/create'">Tạo tiếp</button>
        <button class="btn" onclick="location.href='<%=request.getContextPath()%>/app/request/list?scope=mine'">Về “My Requests”</button>
      </div>
    </div>
  </div>
<% } %>

<script>
  const selType   = document.getElementById('typeId');
  const grpReason = document.getElementById('reasonGroup');
  const txtReason = document.getElementById('reason');
  const inpFrom   = document.getElementById('from');
  const inpTo     = document.getElementById('to');

  // Không cho chọn ngày quá khứ
  (function lockPast(){
    const today = new Date();
    const y = today.getFullYear();
    const m = String(today.getMonth()+1).padStart(2,'0');
    const d = String(today.getDate()).padStart(2,'0');
    const min = `${y}-${m}-${d}`;
    inpFrom.min = min;
    inpTo.min   = min;
  })();

  // Khi chọn “Khác” -> hiện Reason (dựa theo text hoặc data-code="OTHER")
  function toggleReason(){
    const opt = selType.options[selType.selectedIndex];
    const code = (opt.getAttribute('data-code') || '').toUpperCase();
    const text = opt.textContent.trim().toLowerCase();

    const isOther = code === 'OTHER' || text.includes('khác');
    if (isOther){
      grpReason.classList.remove('hidden');
      txtReason.setAttribute('required','required');
    }else{
      grpReason.classList.add('hidden');
      txtReason.removeAttribute('required');
      txtReason.value = '';
    }
  }
  selType.addEventListener('change', toggleReason);
  // Gọi 1 lần để áp trạng thái ban đầu
  toggleReason();
</script>
</body>
</html>
