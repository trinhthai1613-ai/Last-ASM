<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.LeaveType" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Tạo đơn nghỉ phép</title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/style.css"/>
</head>
<body>
<%
  @SuppressWarnings("unchecked")
  List<LeaveType> types = (List<LeaveType>) request.getAttribute("types");
  if (types == null) types = new ArrayList<>();

  String createdId = request.getParameter("createdId");
  String errorMsg = (String) request.getAttribute("error");

  java.time.LocalDate today = java.time.LocalDate.now();
  String todayStr = today.toString();
%>

<main class="container">
  <section class="pixel-card">
    <h1 class="pixel-heading">Tạo đơn nghỉ phép</h1>

    <% if (errorMsg != null && !errorMsg.isBlank()) { %>
      <div class="pixel-alert error" role="alert">
        <strong>Lỗi:</strong> <%= errorMsg %>
      </div>
    <% } %>

    <form id="frmCreate" method="post" action="<%=request.getContextPath()%>/app/request/create" class="pixel-form">
      <div class="pixel-form-group">
        <label for="typeId">Loại nghỉ phép</label>
        <select class="pixel-input" id="typeId" name="typeId" required>
          <option value="">-- Chọn loại nghỉ --</option>
          <%
            for (LeaveType t : types) {
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

      <div id="reasonGroup" class="pixel-form-group hidden">
        <label for="reason">Lý do</label>
        <textarea class="pixel-input" id="reason" name="reason" placeholder="Nhập lý do cụ thể..."></textarea>
      </div>

      <div class="pixel-form-group pixel-form-inline">
        <div>
          <label for="from">Từ ngày</label>
          <input class="pixel-input" type="date" id="from" name="from" required min="<%= todayStr %>">
        </div>
        <div>
          <label for="to">Đến ngày</label>
          <input class="pixel-input" type="date" id="to" name="to" required min="<%= todayStr %>">
        </div>
      </div>

      <div class="pixel-form-actions">
        <button class="pixel-button" type="submit" id="btnSend">Gửi yêu cầu</button>
        <button class="pixel-button secondary" type="button" id="btnBack">Quay lại</button>
      </div>
    </form>
  </section>
</main>

<script>
  (function () {
    var btn = document.getElementById('btnBack');
    if (!btn) return;
    btn.addEventListener('click', function () {
      try {
        if (document.referrer && new URL(document.referrer).origin === location.origin) {
          history.back();
          return;
        }
      } catch (e) {}
      location.href = '<%=request.getContextPath()%>/app/request/list?scope=mine';
    });
  })();
</script>

<% if (createdId != null && !createdId.isBlank()) { %>
  <div class="pixel-modal-backdrop"></div>
  <div class="pixel-modal">
    <div class="pixel-card">
      <h3 class="pixel-heading" style="font-size: 1.25rem;">Tạo đơn thành công!</h3>
      <p>Mã yêu cầu: <strong><%= createdId %></strong></p>
      <div class="pixel-form-actions">
        <button class="pixel-button" onclick="location.href='<%=request.getContextPath()%>/app/request/create'">Tạo thêm</button>
        <button class="pixel-button secondary" onclick="location.href='<%=request.getContextPath()%>/app/request/list?scope=mine'">Về “Yêu cầu của tôi”</button>
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

  (function lockPast(){
    const today = new Date();
    const y = today.getFullYear();
    const m = String(today.getMonth()+1).padStart(2,'0');
    const d = String(today.getDate()).padStart(2,'0');
    const min = `${y}-${m}-${d}`;
    inpFrom.min = min;
    inpFrom.setAttribute('min', min);
    inpTo.min   = min;
    inpTo.setAttribute('min', min);
  })();

  function syncToMin(){
    const base = inpFrom.value || inpFrom.min;
    if (base) {
      inpTo.min = base;
      if (inpTo.value && inpTo.value < base) {
        inpTo.value = base;
      }
    }
  }

  inpFrom.addEventListener('change', syncToMin);
  syncToMin();

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
  toggleReason();
</script>
</body>
</html>
