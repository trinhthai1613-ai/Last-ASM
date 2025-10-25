<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.LeaveType" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Create Leave Request</title>
</head>
<body style="font-family:Arial, sans-serif">
<h2>Create Leave Request</h2>

<p style="color:green"><%= (request.getAttribute("MSG")!=null?request.getAttribute("MSG"):"") %></p>
<p style="color:red"><%= (request.getAttribute("ERROR")!=null?request.getAttribute("ERROR"):"") %></p>

<%
  List<LeaveType> types = (List<LeaveType>) request.getAttribute("types");
  String sel = (String) request.getAttribute("typeId");
  boolean hasSel = (sel != null && !sel.isEmpty());
  String today = java.time.LocalDate.now().toString(); // yyyy-MM-dd
%>

<form method="get" action="">
  <label>Leave Type:</label>
  <select name="typeId" onchange="this.form.submit()" required>
    <% if (!hasSel) { %>
      <option value="">-- Select type --</option>
    <% } %>
    <% if (types != null) {
         for (LeaveType t : types) { %>
      <option value="<%=t.getLeaveTypeId()%>"
              data-code="<%=t.getTypeCode()%>"
              <%= (hasSel && sel.equals(String.valueOf(t.getLeaveTypeId())) ? "selected": "") %>>
        <%= (t.getTypeCode()!=null?t.getTypeCode():"") %> - <%= (t.getTypeName()!=null?t.getTypeName():"") %>
      </option>
    <% } } %>
  </select>
</form>

<form method="post" action="create" id="createForm">
  <input type="hidden" name="leaveTypeId" value="<%= (hasSel?sel:"") %>"/>

  <div id="reason-option-wrapper">
    <label>Reason option:</label>
    <select name="reasonOptionId">
      <option value="">(none)</option>
      <%
        String[][] reasons = (String[][]) request.getAttribute("reasons");
        if (reasons != null) {
          for (String[] r : reasons) {
      %>
        <option value="<%=r[0]%>"><%=r[1]%></option>
      <%  } } %>
    </select>
  </div>

  <div id="reason-text-wrapper" style="display:none; margin-top:6px;">
    <label>Reason:</label><br/>
    <textarea name="reason" rows="3" cols="60"></textarea>
  </div>

  <div style="margin-top:8px;">
    <label>From:</label>
    <input type="date" name="fromDate" id="fromDate" required min="<%=today%>"/>
    <label style="margin-left:12px">To:</label>
    <input type="date" name="toDate" id="toDate" required min="<%=today%>"/>
  </div>

  <div style="margin-top:10px;">
    <button id="sendBtn" type="submit" <%= (!hasSel ? "disabled" : "") %>>Send</button>
    <a href="<%=request.getContextPath()%>/app/home" style="margin-left:12px;">Back</a>
  </div>
</form>

<script>
(function(){
  var select = document.querySelector('select[name="typeId"]');
  var reasonOptWrap  = document.getElementById('reason-option-wrapper');
  var reasonTextWrap = document.getElementById('reason-text-wrapper');
  var reasonText     = document.querySelector('textarea[name="reason"]');
  var from = document.getElementById('fromDate');
  var to   = document.getElementById('toDate');
  var sendBtn = document.getElementById('sendBtn');

  function isOtherSelected() {
    if (!select || select.selectedIndex < 0) return false;
    var opt = select.options[select.selectedIndex];
    var code = (opt.getAttribute('data-code') || '').toUpperCase();
    var label = (opt.text || '').toLowerCase();
    return code === 'OTHER' || label.indexOf('khác') >= 0 || label.indexOf('khac') >= 0;
  }

  function syncReasonUI() {
    var other = isOtherSelected();
    if (other) {
      if (reasonOptWrap)  reasonOptWrap.style.display = 'none';
      if (reasonTextWrap) reasonTextWrap.style.display = '';
      if (reasonText)     reasonText.required = true;
    } else {
      if (reasonOptWrap)  reasonOptWrap.style.display = '';
      if (reasonTextWrap) reasonTextWrap.style.display = 'none';
      if (reasonText)     reasonText.required = false;
    }
  }

  function syncDates(){
    if (!from || !to) return;
    if (from.value) {
      to.min = from.value;               // to >= from
      if (to.value && to.value < from.value) to.value = from.value;
    }
  }

  function syncSendBtn() {
    if (!sendBtn || !select) return;
    sendBtn.disabled = (select.value === "");
  }

  if (select)  select.addEventListener('change', function(){ syncReasonUI(); syncSendBtn(); });
  if (from)    from.addEventListener('change', syncDates);

  // trạng thái ban đầu sau khi forward từ server
  syncReasonUI();
  syncDates();
  syncSendBtn();
})();
</script>

</body>
</html>
