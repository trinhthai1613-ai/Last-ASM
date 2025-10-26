<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*, com.leavemgmt.model.AuditLog" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Nhật ký kiểm toán</title>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/style.css"/>
</head>
<body>
<%
  @SuppressWarnings("unchecked")
  List<AuditLog> logs = (List<AuditLog>) request.getAttribute("logs");

  java.util.function.Function<String,String> mapStatus = (code) -> {
    if (code == null) return "";
    String c = code.trim().toUpperCase(Locale.ROOT);
    switch (c) {
      case "INPROGRESS": return "Đang xử lý";
      case "APPROVED":   return "Đã duyệt";
      case "REJECTED":   return "Từ chối";
      default:           return code;
    }
  };

  java.util.function.Function<String,String> trimOrEmpty = (value) -> {
    if (value == null) return "";
    StringBuilder cleaned = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '\u00A0':
        case '\u2007':
        case '\u202F':
        case '\u200B':
        case '\u200C':
        case '\u200D':
        case '\uFEFF':
          cleaned.append(' ');
          break;
        default:
          if (Character.isISOControl(ch)) {
            continue;
          }
          cleaned.append(Character.isWhitespace(ch) ? ' ' : ch);
      }
    }
    return cleaned.toString().trim();
  };

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
    return left.isEmpty() ? right : left;
  };
%>

<main class="container">
  <section class="pixel-card">
    <h1 class="pixel-heading">Nhật ký kiểm toán</h1>
    <div class="pixel-form-actions" style="justify-content:flex-start; margin-bottom:1.5rem;">
      <button class="pixel-button secondary" type="button" id="btnBack">Quay về</button>
      <a class="pixel-button secondary" href="<%=request.getContextPath()%>/app/home">Bảng điều khiển</a>
    </div>

    <div style="overflow-x:auto;">
      <table class="pixel-table">
        <thead>
          <tr>
            <th>Thời gian (UTC)</th>
            <th>Thao tác viên</th>
            <th>Hành động</th>
            <th>Mã yêu cầu</th>
            <th>Trạng thái</th>
          </tr>
        </thead>
        <tbody>
        <%
          boolean hasRow = false;
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
                continue;
              }
              hasRow = true;
        %>
          <tr>
            <td><%= a.getOccurredAt() == null ? "" : a.getOccurredAt() %></td>
            <td><%= actor %></td>
            <td><%= action %></td>
            <td><%= reqCode %></td>
            <td><%= status %></td>
          </tr>
        <%
            }
          }
          if (!hasRow) {
        %>
          <tr>
            <td colspan="5" style="text-align:center; padding:2rem; color: var(--text-muted);">
              Không có bản ghi nào.
            </td>
          </tr>
        <%
          }
        %>
        </tbody>
      </table>
    </div>
  </section>
</main>

<script>
  (function(){
    var backBtn = document.getElementById('btnBack');
    if (!backBtn) return;
    backBtn.addEventListener('click', function(){
      try {
        if (document.referrer && new URL(document.referrer).origin === location.origin) {
          history.back();
          return;
        }
      } catch(e){}
      location.href = '<%=request.getContextPath()%>/app/request/list?scope=mine';
    });
  })();
</script>
</body>
</html>
