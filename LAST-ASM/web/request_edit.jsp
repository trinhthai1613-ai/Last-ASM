<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.LeaveRequest, com.leavemgmt.model.LeaveType" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Edit Leave Request</title>
  <style>
    body{font-family:Arial, sans-serif}
    label{display:inline-block;min-width:110px;margin:8px 0}
    input, select, textarea{padding:6px}
    textarea{width:420px;height:90px}
    .hidden{display:none}
    .error{color:#c62828;margin-bottom:12px}
    .info{margin-bottom:12px;color:#1565c0}
    .btn{padding:6px 12px;border:1px solid #444;background:#fafafa;cursor:pointer}
    form{max-width:640px}
  </style>
</head>
<body>

<%
  LeaveRequest r = (LeaveRequest) request.getAttribute("req");
  if (r == null) {
%>
<p style="color:red">Request not found.</p>
<% return; }

  @SuppressWarnings("unchecked")
  List<LeaveType> types = (List<LeaveType>) request.getAttribute("types");
  if (types == null) types = new ArrayList<>();

  String scope = (String) request.getAttribute("returnScope");
  if (scope == null) scope = "mine";

  String err = (String) request.getAttribute("error");
  Long minutesRemaining = (Long) request.getAttribute("minutesRemaining");
%>

<h2>Edit Request <%= (r.getRequestCode()==null?("#"+r.getRequestId()):r.getRequestCode()) %></h2>

<% if (err != null) { %>
  <div class="error"><%= err %></div>
<% } %>

<div class="info">
  Bạn chỉ có thể chỉnh sửa đơn trong vòng 1 giờ kể từ lúc tạo.
  <% if (minutesRemaining != null && minutesRemaining > 0) { %>
    Còn khoảng <strong><%= minutesRemaining %></strong> phút.
  <% } else { %>
    Hạn chỉnh sửa sắp kết thúc, vui lòng lưu lại ngay nếu cần thay đổi.
  <% } %>
</div>

<form method="post" action="<%=request.getContextPath()%>/app/request/edit">
  <input type="hidden" name="id" value="<%= r.getRequestId() %>">
  <input type="hidden" name="returnScope" value="<%= scope %>">

  <div>
    <label>Leave Type:</label>
    <select id="typeId" name="typeId" required>
      <option value="">-- Select type --</option>
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

  <div id="reasonGroup" class="<%= (r.getReason()==null || r.getReason().isBlank()) ? "hidden" : "" %>">
    <label>Reason:</label>
    <textarea id="reason" name="reason" placeholder="Nhập lý do cụ thể..."><%= r.getReason()==null?"":r.getReason() %></textarea>
  </div>

  <div>
    <label>From:</label>
    <input type="date" id="from" name="from" value="<%= r.getFromDate()==null?"":r.getFromDate() %>" required>
    <label style="min-width:auto;margin-left:10px">To:</label>
    <input type="date" id="to" name="to" value="<%= r.getToDate()==null?"":r.getToDate() %>" required>
  </div>

  <div style="margin-top:10px">
    <button class="btn" type="submit">Save changes</button>
    <button type="button" id="btnCancel">Cancel</button>
  </div>
</form>

<script>
  (function(){
    var btn = document.getElementById('btnCancel');
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