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

  java.util.function.Function<String,String> trimOrEmpty = (value) -> {
    if (value == null) return "";
    StringBuilder cleaned = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '\u00A0': // non-breaking space
        case '\u2007': // figure space
        case '\u202F': // narrow no-break space
        case '\u200B': // zero-width space
        case '\u200C': // zero-width non-joiner
        case '\u200D': // zero-width joiner
        case '\uFEFF': // zero-width no-break space / BOM
          cleaned.append(' ');
          break;
        default:
          if (Character.isISOControl(ch)) {
            // drop other non-printable characters
            continue;
          }
          cleaned.append(Character.isWhitespace(ch) ? ' ' : ch);
      }
    }
    return cleaned.toString().trim();
  };

  // tạo text trạng thái để hiển thị
  java.util.function.Function<AuditLog,String> renderStatus = (a) -> {
    String oldSt = trimOrEmpty.apply(a.getOldStatus());
    String newSt = trimOrEmpty.apply(a.getNewStatus());
    if (oldSt.isEmpty() && newSt.isEmpty()) {
      return "";
    }
    String left  = mapStatus.apply(oldSt);
    String right = mapStatus.apply(newSt);
    if (!left.isEmpty() && !right.isEmpty()) {
      if (left.equalsIgnoreCase(right)) {
        return right;
      }
      return left + " → " + right;
    }
    return left.isEmpty() ? right : left; // 1 trong 2 có thể trống
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

    </tr>
  </thead>
  <tbody>
  <%
    if (logs != null) {
      for (AuditLog a : logs) {
        String actor    = trimOrEmpty.apply(a.getActorName());
        String action   = trimOrEmpty.apply(a.getActionType());
        String reqCode  = trimOrEmpty.apply(a.getRequestCode());
        String note     = trimOrEmpty.apply(a.getNote());
        String status   = trimOrEmpty.apply(renderStatus.apply(a));
        if (status.isEmpty() && (!actor.isEmpty() || !action.isEmpty() || !reqCode.isEmpty() || !note.isEmpty())) {
          status = trimOrEmpty.apply(mapStatus.apply(a.getCurrentStatusCode()));
        }

        if (actor.isEmpty() && action.isEmpty() && reqCode.isEmpty() && note.isEmpty() && status.isEmpty()) {
          continue; // bỏ qua log không có thông tin hiển thị
        }
 %><tr>
      <td><%= a.getOccurredAt() == null ? "" : a.getOccurredAt() %></td>
      <td><%= actor %></td>
      <td><%= action %></td>
      <td><%= reqCode %></td>
      <td><%= status %></td>
  </tr><%
      }
    }
  %>
  </tbody>
</table>
</body>
</html>
