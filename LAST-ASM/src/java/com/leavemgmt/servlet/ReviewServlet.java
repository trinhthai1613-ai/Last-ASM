package com.leavemgmt.servlet;

import com.leavemgmt.dao.RequestDAO;
import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Review (Approve/Reject) a leave request.
 * - Chỉ so sánh trạng thái bằng StatusCode.
 * - Chặn duyệt lại những đơn đã xử lý.
 * - Không cho duyệt đơn của chính mình.
 * - Leaf (nhân sự thấp nhất) không được duyệt.
 */
@WebServlet(name = "ReviewServlet", urlPatterns = {"/app/request/review"})
public class ReviewServlet extends HttpServlet {

    private static final String CODE_INPROGRESS = "INPROGRESS";
    private static final String CODE_APPROVED   = "APPROVED";
    private static final String CODE_REJECTED   = "REJECTED";

    private final RequestDAO requestDAO = new RequestDAO();

    private boolean isProcessed(String statusCode) {
        if (statusCode == null) return false;
        String s = statusCode.trim().toUpperCase();
        return CODE_APPROVED.equals(s) || CODE_REJECTED.equals(s);
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User u = (User) req.getSession().getAttribute("LOGIN_USER");
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        int id = parseInt(req.getParameter("id"), -1);
        if (id <= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id");
            return;
        }

        LeaveRequest lr = requestDAO.findById(id);
        if (lr == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Đơn đã xử lý thì chỉ xem read-only
        boolean readOnly = isProcessed(lr.getStatusCode());

        req.setAttribute("req", lr);
        req.setAttribute("readOnly", readOnly);
        req.getRequestDispatcher("/review.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User u = (User) req.getSession().getAttribute("LOGIN_USER");
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        // Leaf không được duyệt
        if (u.isLeaf()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Permission denied");
            return;
        }

        int id = parseInt(req.getParameter("id"), -1);
        String decision = req.getParameter("decision"); // "approve" | "reject"

        if (id <= 0 || decision == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        LeaveRequest lr = requestDAO.findById(id);
        if (lr == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Không cho duyệt đơn của chính mình
        if (lr.getCreatedByUserId() == u.getUserId()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot review your own request");
            return;
        }

        // Chặn duyệt lại
        if (isProcessed(lr.getStatusCode())) {
            req.getSession().setAttribute("FLASH_MSG",
                    "Request " + (lr.getRequestCode() == null ? ("#" + lr.getRequestId()) : lr.getRequestCode())
                            + " has already been processed.");
            resp.sendRedirect(req.getContextPath() + "/app/request/list?scope=team");
            return;
        }

        // Ánh xạ quyết định -> StatusCode
        String code;
        if ("approve".equalsIgnoreCase(decision)) {
            code = CODE_APPROVED;
        } else if ("reject".equalsIgnoreCase(decision)) {
            code = CODE_REJECTED;
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown decision");
            return;
        }

        // Cập nhật trạng thái bằng CODE
        requestDAO.updateStatusByCode(id, code);

        // flash message
        String msg = "Request " + (lr.getRequestCode() == null ? ("#" + lr.getRequestId()) : lr.getRequestCode())
                + ("approve".equalsIgnoreCase(decision) ? " has been APPROVED." : " has been REJECTED.");
        req.getSession().setAttribute("FLASH_MSG", msg);

        // quay lại danh sách team (có thể đổi theo nhu cầu)
        resp.sendRedirect(req.getContextPath() + "/app/request/list?scope=team");
    }
}
