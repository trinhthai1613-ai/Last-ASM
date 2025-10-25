package com.leavemgmt.servlet;

import com.leavemgmt.dao.RequestDAO;
import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.sql.Date;

@WebServlet(name = "RequestCreateServlet", urlPatterns = {"/app/request/create"})
public class RequestCreateServlet extends HttpServlet {

    private final RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User u = (User) req.getSession().getAttribute("LOGIN_USER");

        // Lấy tham số
        int typeId = Integer.parseInt(req.getParameter("typeId"));
        String reason = req.getParameter("reason");
        if (reason != null && reason.isBlank()) reason = null;

        LocalDate fromLd = LocalDate.parse(req.getParameter("from"));
        LocalDate toLd   = LocalDate.parse(req.getParameter("to"));

        // Gán vào model
        LeaveRequest r = new LeaveRequest();
        r.setLeaveTypeId(typeId);
        r.setReason(reason);                           // QUAN TRỌNG
        r.setFromDate(Date.valueOf(fromLd));
        r.setToDate(Date.valueOf(toLd));
        r.setCreatedByUserId(u.getUserId());           // QUAN TRỌNG

        // Tạo đơn
        int newId = requestDAO.createRequest(r);

        // Redirect (bạn đang bật popup thành công ở trang create)
        resp.sendRedirect(req.getContextPath()+"/app/request/create?createdId="+newId);
    }
}
