<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.LeaveRequest, com.leavemgmt.model.LeaveType" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Chỉnh sửa yêu cầu nghỉ phép</title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/style.css"/>
</head>
<body>
<%
  LeaveRequest r = (LeaveRequest) request.getAttribute("req");
  if (r == null) {
%>
  <main class="pixel-layout">
    <section class="pixel-card" style="max-width:420px;">
      <div class="pixel-alert error">Không tìm thấy yêu cầu.</div>
      <a class="pixel-link" href="<%=request.getContextPath()%>/app/request/list?scope=mine">Quay về danh sách</a>
    </section>
  </main>
<% return; }

  @SuppressWarnings("unchecked")
  List<LeaveType> types = (List<LeaveType>) request.getAttribute("types");
  if (types == null) types = new ArrayList<>();

  String scope = (String) request.getAttribute("returnScope");
  if (scope == null) scope = "mine";

  String err = (String) request.getAttribute("error");
  Long minutesRemaining = (Long) request.getAttribute("minutesRemaining");
%>

<main class="container">
  <section class="pixel-card">
    <h1 class="pixel-heading">Chỉnh sửa yêu cầu <%= (r.getRequestCode()==null?("#"+r.getRequestId()):r.getRequestCode()) %></h1>

    <% if (err != null && !err.isBlank()) { %>
      <div class="pixel-alert error"><%= err %></div>
    <% } %>

    <div class="pixel-alert info">
      Bạn chỉ có thể chỉnh sửa đơn trong vòng 1 giờ kể từ lúc tạo.
      <% if (minutesRemaining != null && minutesRemaining > 0) { %>
        Còn khoảng <strong><%= minutesRemaining %></strong> phút.
      <% } else { %>
        Hạn chỉnh sửa sắp kết thúc, vui lòng lưu lại ngay nếu cần thay đổi.
      <% } %>
    </div>

    <form method="post" action="<%=request.getContextPath()%>/app/request/edit" class="pixel-form">
      <input type="hidden" name="id" value="<%= r.getRequestId() %>">
      <input type="hidden" name="returnScope" value="<%= scope %>">

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
              boolean selected = t.getLeaveTypeId() == r.getLeaveTypeId();
          %>
            <option value="<%=t.getLeaveTypeId()%>" data-code="<%=code%>" <%= selected ? "selected" : "" %>>
              <%= t.getTypeName() %>
            </option>
          <% } %>
        </select>
      </div>

      <div id="reasonGroup" class="pixel-form-group <%= (r.getReason()==null || r.getReason().isBlank()) ? "hidden" : "" %>">
        <label for="reason">Lý do</label>
        <textarea class="pixel-input" id="reason" name="reason" placeholder="Nhập lý do cụ thể..."><%= r.getReason()==null?"":r.getReason() %></textarea>
      </div>

      <div class="pixel-form-group pixel-form-inline">
        <div>
          <label for="from">Từ ngày</label>
          <input class="pixel-input" type="date" id="from" name="from" value="<%= r.getFromDate()==null?"":r.getFromDate() %>" required>
        </div>
        <div>
          <label for="to">Đến ngày</label>
          <input class="pixel-input" type="date" id="to" name="to" value="<%= r.getToDate()==null?"":r.getToDate() %>" required>
        </div>
      </div>

      <div class="pixel-form-actions">
        <button class="pixel-button" type="submit">Lưu thay đổi</button>
        <button class="pixel-button secondary" type="button" id="btnCancel">Hủy</button>
      </div>
    </form>
  </section>
</main>

<script>
  (function(){
    var btn = document.getElementById('btnCancel');
    if (!btn) return;
    btn.addEventListener('click', function(){
      try {
        if (document.referrer && new URL(document.referrer).origin === location.origin) {
          history.back();
          return;
        }
      } catch(e){}
      location.href = '<%=request.getContextPath()%>/app/request/list?scope=<%= scope %>';
    });
  })();

  const selType   = document.getElementById('typeId');
  const grpReason = document.getElementById('reasonGroup');
  const txtReason = document.getElementById('reason');
  const inpFrom   = document.getElementById('from');
  const inpTo     = document.getElementById('to');

  (function enforceMinDates(){
    const today = new Date();
    const y = today.getFullYear();
    const m = String(today.getMonth()+1).padStart(2,'0');
    const d = String(today.getDate()).padStart(2,'0');
    const todayStr = `${y}-${m}-${d}`;

    function syncTo(){
      const base = (inpFrom.value && inpFrom.value > todayStr) ? inpFrom.value : todayStr;
      inpTo.min = base;
      if (inpTo.value && inpTo.value < base) {
        inpTo.value = base;
      }
    }

    if (!inpFrom.value || inpFrom.value < todayStr) {
      inpFrom.value = todayStr;
    }
    inpFrom.min = todayStr;
    syncTo();

    inpFrom.addEventListener('change', function(){
      if (inpFrom.value && inpFrom.value < todayStr) {
        inpFrom.value = todayStr;
      }
      syncTo();
    });
  })();

  function toggleReason(){
    const opt = selType.options[selType.selectedIndex];
    if (!opt){
      grpReason.classList.add('hidden');
      txtReason.removeAttribute('required');
      return;
    }
    const code = (opt.getAttribute('data-code') || '').toUpperCase();
    const text = opt.textContent.trim().toLowerCase();
    const isOther = code === 'OTHER' || text.includes('khác');
    if (isOther){
      grpReason.classList.remove('hidden');
      txtReason.setAttribute('required','required');
    } else {
      if (!txtReason.value.trim()){
        grpReason.classList.add('hidden');
      }
      txtReason.removeAttribute('required');
    }
  }

  selType.addEventListener('change', toggleReason);
  toggleReason();
</script>
</body>
</html>
