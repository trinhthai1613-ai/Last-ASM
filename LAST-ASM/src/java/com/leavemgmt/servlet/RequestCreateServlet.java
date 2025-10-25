package com.leavemgmt.servlet;

import com.leavemgmt.dao.RequestDAO;
import com.leavemgmt.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;

@WebServlet(name="RequestCreateServlet", urlPatterns={"/app/request/create"})
public class RequestCreateServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // chỉ forward JSP; popup sẽ tự hiển thị nếu có ?createdId=...
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
            int leaveTypeId = Integer.parseInt(request.getParameter("typeId")); // đã kiểm soát ở JSP
            String reason = request.getParameter("reason");
            String reasonOpt = request.getParameter("reasonOpt");   // có thể null
            Integer reasonOptionId = (reasonOpt == null || reasonOpt.isEmpty()) ? null : Integer.parseInt(reasonOpt);

            Date from = Date.valueOf(request.getParameter("fromDate"));
            Date to   = Date.valueOf(request.getParameter("toDate"));

            // Gọi DAO tạo đơn
            int createdId = new RequestDAO().createRequest(u.getUserId(), leaveTypeId, reasonOptionId, from, to, reason);

            // -> Redirect để tránh resubmit và hiển thị popup
            response.sendRedirect(request.getContextPath() + "/app/request/create?createdId=" + createdId);
        } catch (Exception ex) {
            // có lỗi -> flash + quay lại create rỗng
            request.getSession().setAttribute("FLASH_MSG",
                    "Create failed: " + ex.getMessage());
            response.sendRedirect(request.getContextPath() + "/app/request/create");
        }
    }
}
