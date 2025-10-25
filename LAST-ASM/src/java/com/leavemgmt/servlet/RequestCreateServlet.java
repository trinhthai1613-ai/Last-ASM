package com.leavemgmt.servlet;

import com.leavemgmt.dao.RequestDAO;
import com.leavemgmt.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

@WebServlet(name="RequestCreateServlet", urlPatterns={"/app/request/create"})
public class RequestCreateServlet extends HttpServlet {

    @Override

protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    java.util.List<com.leavemgmt.model.LeaveType> types =
            new com.leavemgmt.dao.LeaveTypeDAO().listActive();
    request.setAttribute("types", types);
    request.getRequestDispatcher("/request_create.jsp").forward(request, response);
}




    @Override
protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    User u = (User) request.getSession().getAttribute("LOGIN_USER");
    if (u == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    try {
        String typeIdStr = request.getParameter("typeId");
        if (typeIdStr == null || typeIdStr.isBlank()) throw new IllegalArgumentException("Vui lòng chọn loại nghỉ.");
        int leaveTypeId = Integer.parseInt(typeIdStr);

        String fromStr = request.getParameter("fromDate");
        String toStr   = request.getParameter("toDate");
        if (fromStr == null || toStr == null || fromStr.isBlank() || toStr.isBlank())
            throw new IllegalArgumentException("Vui lòng chọn ngày From/To.");

        java.sql.Date from = java.sql.Date.valueOf(fromStr);
        java.sql.Date to   = java.sql.Date.valueOf(toStr);

        String reason = request.getParameter("reason");
        String ro = request.getParameter("reasonOpt");
        Integer reasonOptionId = null;
        if (ro != null && !ro.isBlank()) {
            try { reasonOptionId = Integer.valueOf(ro); } catch (NumberFormatException ignore) { /* dùng null */ }
        }

        int createdId = new com.leavemgmt.dao.RequestDAO()
                .createRequest(u.getUserId(), leaveTypeId, reasonOptionId, from, to, reason);

        if (createdId <= 0) throw new RuntimeException("Không lấy được RequestID.");

        // Thành công -> hiện popup
        response.sendRedirect(request.getContextPath() + "/app/request/create?createdId=" + createdId);
    } catch (Exception ex) {
        request.getSession().setAttribute("FLASH_MSG", "Create failed: " + ex.getMessage());
        response.sendRedirect(request.getContextPath() + "/app/request/create");
    }
}

}
