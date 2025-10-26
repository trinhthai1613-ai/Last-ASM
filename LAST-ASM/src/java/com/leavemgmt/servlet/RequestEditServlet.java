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
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@WebServlet(name = "RequestEditServlet", urlPatterns = {"/app/request/edit"})
public class RequestEditServlet extends HttpServlet {

    private final RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User cur = (User) req.getSession().getAttribute("LOGIN_USER");
        if (cur == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        int id = Integer.parseInt(req.getParameter("id"));
        LeaveRequest target = requestDAO.findById(id);
        if (target == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String scope = normalizeScope(req.getParameter("scope"));
        if (scope == null) {
            scope = (target.getCreatedByUserId() == cur.getUserId()) ? "mine" : "team";
        }

        if (!canOwnerEdit(cur, target)) {
            if (cur.getUserId() == target.getCreatedByUserId()) {
                req.getSession().setAttribute("FLASH_MSG",
                        "Bạn chỉ có thể chỉnh sửa đơn trong vòng 1 giờ sau khi tạo.");
                resp.sendRedirect(req.getContextPath() + "/app/request/list?scope=" + scope);
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            return;
        }

        req.setAttribute("req", target);
        req.setAttribute("types", requestDAO.listTypes());
        req.setAttribute("returnScope", scope);
        req.setAttribute("minutesRemaining", Long.valueOf(calculateMinutesRemaining(target)));

        req.getRequestDispatcher("/request_edit.jsp").forward(req, resp);
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

        String scope = normalizeScope(req.getParameter("returnScope"));
        if (scope == null) {
            scope = (existing.getCreatedByUserId() == cur.getUserId()) ? "mine" : "team";
        }

        if (!canOwnerEdit(cur, existing)) {
            if (cur.getUserId() == existing.getCreatedByUserId()) {
                req.getSession().setAttribute("FLASH_MSG",
                        "Đơn đã quá hạn 1 giờ nên không thể chỉnh sửa nữa.");
                resp.sendRedirect(req.getContextPath() + "/app/request/list?scope=" + scope);
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            return;
        }

        try {
            int typeId = Integer.parseInt(req.getParameter("typeId"));
            String reason = req.getParameter("reason");
            LocalDate fromLd = LocalDate.parse(req.getParameter("from"));
            LocalDate toLd   = LocalDate.parse(req.getParameter("to"));

            if (toLd.isBefore(fromLd)) {
                throw new IllegalArgumentException("To date must be after or equal to From date");
            }

            LeaveRequest update = new LeaveRequest();
            update.setRequestId(existing.getRequestId());
            update.setLeaveTypeId(typeId);
            update.setReason((reason == null || reason.isBlank()) ? null : reason.trim());
            update.setFromDate(Date.valueOf(fromLd));
            update.setToDate(Date.valueOf(toLd));

            String note = req.getParameter("note");

            requestDAO.updateRequest(update, cur.getUserId(), note);

            req.getSession().setAttribute("FLASH_MSG", "Request #" + id + " has been updated.");
            resp.sendRedirect(req.getContextPath() + "/app/request/list?scope=" + scope);
        } catch (Exception ex) {
            req.setAttribute("error", "Update failed: " + ex.getMessage());

            // preserve submitted values
            try {
                int typeId = Integer.parseInt(req.getParameter("typeId"));
                existing.setLeaveTypeId(typeId);
            } catch (Exception ignore) {}

            String reason = req.getParameter("reason");
            existing.setReason((reason == null || reason.isBlank()) ? null : reason.trim());

            try {
                existing.setFromDate(Date.valueOf(req.getParameter("from")));
                existing.setToDate(Date.valueOf(req.getParameter("to")));
            } catch (Exception ignore) {}

            req.setAttribute("req", existing);
            req.setAttribute("types", requestDAO.listTypes());
            req.setAttribute("returnScope", scope);
            req.setAttribute("minutesRemaining", Long.valueOf(calculateMinutesRemaining(existing)));

            req.getRequestDispatcher("/request_edit.jsp").forward(req, resp);
        }
    }

    private boolean canOwnerEdit(User actor, LeaveRequest target) {
        if (actor == null || target == null) return false;
        if (actor.getUserId() != target.getCreatedByUserId()) return false;

        String status = target.getStatusCode();
        if (status == null || !"INPROGRESS".equalsIgnoreCase(status)) {
            return false;
        }

        return isWithinOwnerWindow(target);
    }

    private String normalizeScope(String raw) {
        if (raw == null) return null;
        if ("team".equalsIgnoreCase(raw)) return "team";
        if ("mine".equalsIgnoreCase(raw)) return "mine";
        return null;
    }

    private boolean isWithinOwnerWindow(LeaveRequest target) {
        if (target == null || target.getCreatedAt() == null) {
            return false;
        }

        Instant created = target.getCreatedAt().toInstant();
        Instant now = Instant.now();
        Duration elapsed = Duration.between(created, now);
        if (elapsed.isNegative()) {
            elapsed = Duration.ZERO;
        }

        return elapsed.compareTo(Duration.ofMinutes(RequestDAO.OWNER_EDIT_WINDOW_MINUTES)) <= 0;
    }

    private long calculateMinutesRemaining(LeaveRequest target) {
        if (target == null || target.getCreatedAt() == null) {
            return 0;
        }

        if (!isWithinOwnerWindow(target)) {
            return 0;
        }

        Instant created = target.getCreatedAt().toInstant();
        Duration elapsed = Duration.between(created, Instant.now());
        if (elapsed.isNegative()) {
            elapsed = Duration.ZERO;
        }

        Duration window = Duration.ofMinutes(RequestDAO.OWNER_EDIT_WINDOW_MINUTES);
        Duration remaining = window.minus(elapsed);
        if (remaining.isNegative()) {
            return 0;
        }

        long seconds = remaining.getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return (seconds + 59) / 60;
    }
}