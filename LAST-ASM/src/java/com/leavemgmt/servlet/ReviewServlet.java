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

@WebServlet(name = "ReviewServlet", urlPatterns = {"/app/request/review"})
public class ReviewServlet extends HttpServlet {

    private final RequestDAO requestDAO = new RequestDAO();

    // Chuẩn hóa dùng CODE, không so sánh tên hiển thị
   
    private static final String CODE_APPROVED   = "APPROVED";
    private static final String CODE_REJECTED   = "REJECTED";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User cur = (User) req.getSession().getAttribute("LOGIN_USER");
        if (cur == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        int id = Integer.parseInt(req.getParameter("id"));
        LeaveRequest lr = requestDAO.findById(id);
        if (lr == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!canReview(cur, lr)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        req.setAttribute("req", lr);
        req.getRequestDispatcher("/review.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User cur = (User) req.getSession().getAttribute("LOGIN_USER");
        if (cur == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        int id = Integer.parseInt(req.getParameter("id"));
        LeaveRequest existing = requestDAO.findById(id);
        if (existing == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String decision = req.getParameter("decision"); // approve | reject

        if (!canReview(cur, existing)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // map quyết định sang STATUS CODE
        String code;
        if ("approve".equalsIgnoreCase(decision)) {
            code = CODE_APPROVED;
        } else if ("reject".equalsIgnoreCase(decision)) {
            code = CODE_REJECTED;
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown decision");
            return;
        }

        // Cập nhật trạng thái bằng CODE (và ghi log nếu bạn đã thêm)
      requestDAO.updateStatusByCode(id, code, cur.getUserId(), null);

        // flash + quay lại danh sách team
       String msg = "Request #" + id + " status updated to " + code + ".";
        req.getSession().setAttribute("FLASH_MSG", msg);

        resp.sendRedirect(req.getContextPath() + "/app/request/list?scope=team");
    }
    private boolean canReview(User actor, LeaveRequest target) {
        if (actor == null || target == null) {
            return false;
        }
        if (actor.isTopLevel()) {
            return true;
        }
        if (actor.getUserId() == target.getCreatedByUserId()) {
            return false;
        }
        return requestDAO.isManagerOf(actor.getUserId(), target.getCreatedByUserId());
    }
}
