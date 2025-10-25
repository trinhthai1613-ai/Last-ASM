package com.leavemgmt.servlet;

import com.leavemgmt.dao.RequestDAO;
import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.model.LeaveType;
import com.leavemgmt.model.User;
import com.leavemgmt.util.DBConnection;

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

    // HIỂN THỊ FORM (GET)
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // nạp danh sách loại nghỉ để render combobox
        List<LeaveType> types = requestDAO.listTypes();
        req.setAttribute("types", types);

        // nếu vừa tạo xong -> hiện popup bằng createdId
        String createdId = req.getParameter("createdId");
        if (createdId != null && !createdId.isBlank()) {
            req.setAttribute("createdId", createdId.trim());
        }

        req.getRequestDispatcher("/request_create.jsp").forward(req, resp);
    }

    // NHẬN SUBMIT (POST)
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
            String reason = req.getParameter("reason");      // có thể null nếu không chọn "OTHER"
            LocalDate fromLd = LocalDate.parse(req.getParameter("from"));
            LocalDate toLd   = LocalDate.parse(req.getParameter("to"));

            LeaveRequest r = new LeaveRequest();
            r.setLeaveTypeId(typeId);
            r.setReason((reason == null || reason.isBlank()) ? null : reason.trim());
            r.setFromDate(Date.valueOf(fromLd));
            r.setToDate(Date.valueOf(toLd));
            r.setCreatedByUserId(u.getUserId());

            int newId = requestDAO.createRequest(r);

            // redirect về GET để tránh F5 resubmit + hiện popup
            resp.sendRedirect(req.getContextPath() + "/app/request/create?createdId=" + newId);

        } catch (Exception ex) {
            req.setAttribute("error", "Create failed: " + ex.getMessage());
            doGet(req, resp); // quay lại form kèm lỗi
        }
    }
}
