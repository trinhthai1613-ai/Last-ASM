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

        if (!canEdit(cur, target)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        req.setAttribute("req", target);
        req.setAttribute("types", requestDAO.listTypes());

        String scope = normalizeScope(req.getParameter("scope"));
        if (scope == null) {
            scope = (target.getCreatedByUserId() == cur.getUserId()) ? "mine" : "team";
        }
        req.setAttribute("returnScope", scope);

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

        if (!canEdit(cur, existing)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String scope = normalizeScope(req.getParameter("returnScope"));
        if (scope == null) {
            scope = (existing.getCreatedByUserId() == cur.getUserId()) ? "mine" : "team";
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

            req.getRequestDispatcher("/request_edit.jsp").forward(req, resp);
        }
    }

    private boolean canEdit(User actor, LeaveRequest target) {
        if (actor == null || target == null) return false;

        if (actor.isTopLevel()) return true;
        if (actor.getUserId() == target.getCreatedByUserId()) return true;

        return requestDAO.isManagerOf(actor.getUserId(), target.getCreatedByUserId());
    }

    private String normalizeScope(String raw) {
        if (raw == null) return null;
        if ("team".equalsIgnoreCase(raw)) return "team";
        if ("mine".equalsIgnoreCase(raw)) return "mine";
        return null;
    }
}