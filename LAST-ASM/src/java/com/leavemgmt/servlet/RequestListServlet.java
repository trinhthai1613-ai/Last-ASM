package com.leavemgmt.servlet;

import com.leavemgmt.dao.RequestDAO;
import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name="RequestListServlet", urlPatterns={"/app/request/list"})
public class RequestListServlet extends HttpServlet {

    @Override
protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    com.leavemgmt.model.User u = (com.leavemgmt.model.User) request.getSession().getAttribute("LOGIN_USER");
    boolean isTopLevel = (u != null && u.isTopLevel());
    String scope = request.getParameter("scope");
    com.leavemgmt.dao.RequestDAO dao = new com.leavemgmt.dao.RequestDAO();

    // Top-level: không cho vào "mine" → chuyển sang "team"
    if (isTopLevel && (scope == null || scope.equalsIgnoreCase("mine"))) {
        response.sendRedirect(request.getContextPath() + "/app/request/list?scope=team");
        return;
    }

    if ("team".equalsIgnoreCase(scope)) {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.sql.Date from = java.sql.Date.valueOf(now.withDayOfYear(1));
        java.sql.Date to   = java.sql.Date.valueOf(now.withMonth(12).withDayOfMonth(31));

        java.util.List<com.leavemgmt.model.LeaveRequest> raw = dao.listSubtree(u.getUserId(), from, to);

        // Giữ CHỈ pending + bỏ đơn của chính mình
        java.util.List<com.leavemgmt.model.LeaveRequest> pending = new java.util.ArrayList<>();
        for (com.leavemgmt.model.LeaveRequest r : raw) {
            String st = (r.getStatusName()==null?"":r.getStatusName().trim().toLowerCase());
            boolean isPending = st.equals("inprogress") || st.equals("pending")
                    || st.equals("đang xử lý") || st.equals("dang xu ly");
            boolean isOwn = r.getCreatedBy()!=null && r.getCreatedBy().equalsIgnoreCase(u.getFullName());
            if (isPending && !isOwn) pending.add(r);
        }
        request.setAttribute("list", pending);
        request.setAttribute("scope", "team");
    } else {
        // Chỉ còn nhánh mine cho người KHÔNG top-level
        java.util.List<com.leavemgmt.model.LeaveRequest> list = dao.listMyRequests(u.getUserId());
        request.setAttribute("list", list);
        request.setAttribute("scope", "mine");
    }
    request.getRequestDispatcher("/request_list.jsp").forward(request, response);
}

}
