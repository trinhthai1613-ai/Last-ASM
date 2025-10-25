<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.AuditLog" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Audit Logs</title>
  <style>
    body { font-family: Arial, sans-serif; }
    table { border-collapse: collapse; width: 820px; }
    th, td { border: 1px solid #888; padding: 6px 8px; }
    th { background: #f1f1f1; }
  </style>
</head>
<body>
<%
  @SuppressWarnings("unchecked")
  List<AuditLog> logs = (List<AuditLog>) request.getAttribute("logs");

  // map code -> tiếng Việt
  java.util.function.Function<String,String> mapStatus = (code) -> {
    if (code == null) return "";
    String c = code.trim().toUpperCase(Locale.ROOT);
    switch (c) {
      case "INPROGRESS": return "Đang xử lý";
      case "APPROVED":   return "Đã duyệt";
      case "REJECTED":   return "Từ chối";
      default:           return code; // giữ nguyên nếu code lạ
    }
  };

  // tạo text trạng thái để hiển thị
  java.util.function.Function<AuditLog,String> renderStatus = (a) -> {
    String oldSt = a.getOldStatus();
    String newSt = a.getNewStatus();
    if ((oldSt != null && !oldSt.isBlank()) || (newSt != null && !newSt.isBlank())) {
      String left  = (oldSt == null ? "" : mapStatus.apply(oldSt));
      String right = (newSt == null ? "" : mapStatus.apply(newSt));
      if (!left.isEmpty() && !right.isEmpty()) return left + " → " + right;
      return left + right; // 1 trong 2 có thể trống
    }
    // fallback: dùng trạng thái hiện tại của request
    return mapStatus.apply(a.getCurrentStatusCode());
  };
%>

<h2>Audit Logs</h2>

<!-- nút Back an toàn: về trang trước, nếu không có thì về My Requests -->
<button type="button" id="btnBack">Home</button>
<script>
  (function(){
    document.getElementById('btnBack').onclick = function () {
      try {
        if (document.referrer && new URL(document.referrer).origin === location.origin) {
          history.back(); return;
        }
      } catch(e){}
      location.href = '<%=request.getContextPath()%>/app/request/list?scope=mine';
    };
  })();
</script>

<table>
  <thead>
    <tr>
      <th style="width:150px;">Time (UTC)</th>
      <th style="width:140px;">Actor</th>
      <th style="width:100px;">Action</th>
      <th style="width:140px;">Request Code</th>
      <th>Status</th>
      <th>Note</th>
    </tr>
  </thead>
  <tbody>
  <%
    if (logs != null) {
      for (AuditLog a : logs) {
  %>
    <tr>
      <td><%= a.getOccurredAt() %></td>
      <td><%= a.getActorName() == null ? "" : a.getActorName() %></td>
      <td><%= a.getActionType() %></td>
      <td><%= a.getRequestCode() == null ? "" : a.getRequestCode() %></td>
      <td><%= renderStatus.apply(a) %></td>
      <td><%= a.getNote() == null ? "" : a.getNote() %></td>
    </tr>
  <%
      }
    }
  %>
  </tbody>
</table>
</body>
</html>
