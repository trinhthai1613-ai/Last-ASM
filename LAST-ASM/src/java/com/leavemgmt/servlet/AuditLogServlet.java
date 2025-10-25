package com.leavemgmt.servlet;

import com.leavemgmt.dao.AuditLogDAO;
import com.leavemgmt.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet(name="AuditLogServlet", urlPatterns={"/app/audit/logs"})
public class AuditLogServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User u = (User) request.getSession().getAttribute("LOGIN_USER");
        // Chỉ quản lý (có cấp dưới) và top-level được xem
        if (u == null || (u.isLeaf())) {
            request.getSession().setAttribute("FLASH_MSG", "Bạn không có quyền xem Audit Log.");
            response.sendRedirect(request.getContextPath() + "/app/home");
            return;
        }
        request.setAttribute("logs", new AuditLogDAO().listAll());
        request.getRequestDispatcher("/audit_logs.jsp").forward(request, response);
    }
}
