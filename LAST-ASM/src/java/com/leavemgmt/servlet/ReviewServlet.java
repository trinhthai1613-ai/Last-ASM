package com.leavemgmt.servlet;

import com.leavemgmt.dao.RequestDAO;
import com.leavemgmt.model.LeaveRequest;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "ReviewServlet", urlPatterns = {"/app/request/review"})
public class ReviewServlet extends HttpServlet {

    private final RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int id = Integer.parseInt(req.getParameter("id"));
        LeaveRequest lr = requestDAO.findById(id);
        if (lr == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("req", lr);
        req.getRequestDispatcher("/review.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int id = Integer.parseInt(req.getParameter("id"));
        String decision = req.getParameter("decision"); // "approve" | "reject"

        String code = "INPROGRESS";
        if ("approve".equalsIgnoreCase(decision))      code = "APPROVED";
        else if ("reject".equalsIgnoreCase(decision))  code = "REJECTED";

        requestDAO.updateStatusByCode(id, code);

        resp.sendRedirect(req.getContextPath()+"/app/request/list?scope=team"); // tuỳ hướng trở về
    }
}
