package com.leavemgmt.servlet;

import com.leavemgmt.dao.RequestDAO;
import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet(name="ReviewServlet", urlPatterns={"/app/request/review"})
public class ReviewServlet extends HttpServlet {

    private boolean isProcessed(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.contains("approve") || s.contains("duyệt") || s.contains("duyet")
            || s.contains("reject")  || s.contains("từ chối") || s.contains("tu choi");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User u = (User) request.getSession().getAttribute("LOGIN_USER");
        int id = Integer.parseInt(request.getParameter("id"));
        LeaveRequest lr = new RequestDAO().getRequest(id);
        if (lr == null) {
            request.getSession().setAttribute("FLASH_MSG", "Không tìm thấy đơn.");
            response.sendRedirect(request.getContextPath() + "/app/request/list?scope=team");
            return;
        }
        if ((lr.getCreatedBy()!=null && lr.getCreatedBy().equalsIgnoreCase(u.getFullName())) || isProcessed(lr.getStatusName())) {
            request.getSession().setAttribute("FLASH_MSG",
                (lr.getCreatedBy()!=null && lr.getCreatedBy().equalsIgnoreCase(u.getFullName()))
                    ? "Trưởng bộ phận không thể duyệt đơn của chính mình."
                    : "Đơn đã được xử lý, không thể duyệt lại.");
            response.sendRedirect(request.getContextPath() + "/app/request/list?scope=team");
            return;
        }
        request.setAttribute("req", lr);
        request.getRequestDispatcher("/review.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User u = (User) request.getSession().getAttribute("LOGIN_USER");
        int id = Integer.parseInt(request.getParameter("id"));
        String action = request.getParameter("action");
        LeaveRequest lr = new RequestDAO().getRequest(id);
        if (lr == null) {
            request.getSession().setAttribute("FLASH_MSG", "Không tìm thấy đơn.");
            response.sendRedirect(request.getContextPath() + "/app/request/list?scope=team");
            return;
        }
        if ((lr.getCreatedBy()!=null && lr.getCreatedBy().equalsIgnoreCase(u.getFullName())) || isProcessed(lr.getStatusName())) {
            request.getSession().setAttribute("FLASH_MSG",
                (lr.getCreatedBy()!=null && lr.getCreatedBy().equalsIgnoreCase(u.getFullName()))
                    ? "Trưởng bộ phận không thể duyệt đơn của chính mình."
                    : "Đơn đã được xử lý, không thể duyệt lại.");
            response.sendRedirect(request.getContextPath() + "/app/request/list?scope=team");
            return;
        }
        boolean approve = "approve".equalsIgnoreCase(action);
        String note = request.getParameter("note");
        new RequestDAO().review(u.getUserId(), id, approve, note);
        request.getSession().setAttribute("FLASH_MSG", approve ? "Đã duyệt đơn." : "Đã từ chối đơn.");
        response.sendRedirect(request.getContextPath() + "/app/request/list?scope=team");
    }
}
