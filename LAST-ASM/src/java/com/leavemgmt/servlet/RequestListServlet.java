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
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "RequestListServlet", urlPatterns = {"/app/request/list"})
public class RequestListServlet extends HttpServlet {

    private final RequestDAO dao = new RequestDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User u = (User) request.getSession().getAttribute("LOGIN_USER");
        if (u == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String scope = request.getParameter("scope");
        if (scope == null || scope.isBlank()) scope = "mine";

        if (u.isLeaf() && "team".equalsIgnoreCase(scope)) {
            response.sendRedirect(request.getContextPath() + "/app/request/list?scope=mine");
            return;
        }

        if ("team".equalsIgnoreCase(scope)) {
            LocalDate now = LocalDate.now();
            Date from = Date.valueOf(now.withDayOfYear(1));
            Date to   = Date.valueOf(now.withMonth(12).withDayOfMonth(31));

            List<LeaveRequest> raw = dao.listSubtree(u.getUserId(), from, to);

            List<LeaveRequest> pending = new ArrayList<>();
            for (LeaveRequest r : raw) {

                // So sánh CODE duy nhất
                boolean isPending = "INPROGRESS".equalsIgnoreCase(r.getStatusCode());

                // Không hiện đơn của chính mình
                boolean isOwn = r.getCreatedByUserId() == u.getUserId();

                if (isPending && !isOwn) pending.add(r);
            }

            request.setAttribute("list", pending);
            request.setAttribute("scope", "team");
        } else {
            List<LeaveRequest> list = dao.listMyRequests(u.getUserId());
            request.setAttribute("list", list);
            request.setAttribute("scope", "mine");
        }

        request.getRequestDispatcher("/request_list.jsp").forward(request, response);
    }
}
