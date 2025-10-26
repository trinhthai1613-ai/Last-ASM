package com.leavemgmt.servlet;

import com.leavemgmt.dao.RequestDAO;
import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.model.LeaveType;
import com.leavemgmt.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "RequestCreateServlet", urlPatterns = {"/app/request/create"})
public class RequestCreateServlet extends HttpServlet {

    private final RequestDAO requestDAO = new RequestDAO();

    // HIỂN THỊ FORM + POPUP nếu có createdId
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // nạp danh sách loại nghỉ
        List<LeaveType> types = requestDAO.listTypes();
        req.setAttribute("types", types);

        // nếu vừa tạo xong sẽ có createdId -> JSP sẽ bật popup
        String createdId = req.getParameter("createdId");
        if (createdId != null && !createdId.trim().isEmpty()) {
            req.setAttribute("createdId", createdId.trim());
        }

        req.getRequestDispatcher("/request_create.jsp").forward(req, resp);
    }

    // NHẬN SUBMIT
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User u = (User) req.getSession().getAttribute("LOGIN_USER");
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            int typeId = Integer.parseInt(req.getParameter("typeId"));
            String reason = req.getParameter("reason"); // có thể null khi KHÁC không được chọn
            LocalDate fromLd = LocalDate.parse(req.getParameter("from"));
            LocalDate toLd   = LocalDate.parse(req.getParameter("to"));
            LocalDate today = LocalDate.now();
            if (fromLd.isBefore(today)) {
                throw new IllegalArgumentException("Start date must be today or later");
            }
            if (toLd.isBefore(fromLd)) {
                throw new IllegalArgumentException("End date must be on or after the start date");
            }

            LeaveRequest r = new LeaveRequest();
            r.setLeaveTypeId(typeId);
            r.setReason((reason != null && !reason.isBlank()) ? reason.trim() : null);
            r.setFromDate(Date.valueOf(fromLd));
            r.setToDate(Date.valueOf(toLd));
            r.setCreatedByUserId(u.getUserId());

            int newId = requestDAO.createRequest(r);

            // chuyển về GET kèm createdId để hiển thị popup thành công
            resp.sendRedirect(req.getContextPath() + "/app/request/create?createdId=" + newId);

        } catch (Exception ex) {
            // lỗi validate đơn giản -> quay lại form
            req.setAttribute("error", "Create failed: " + ex.getMessage());
            doGet(req, resp);
        }
    }
}
