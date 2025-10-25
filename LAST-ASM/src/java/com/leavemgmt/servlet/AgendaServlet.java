package com.leavemgmt.servlet;

import com.leavemgmt.dao.AgendaDAO;
import com.leavemgmt.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

@WebServlet(name = "AgendaServlet", urlPatterns = {"/app/agenda"})
public class AgendaServlet extends HttpServlet {
    private final AgendaDAO dao = new AgendaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User u = (User) request.getSession().getAttribute("LOGIN_USER");
        if (u != null && u.isLeaf()) { // nhân viên cấp thấp nhất -> cấm
            request.getSession().setAttribute("FLASH_MSG", "Bạn không có quyền xem Agenda.");
            response.sendRedirect(request.getContextPath() + "/app/home");
            return;
        }

        List<String[]> divisions = dao.listDivisions();
        request.setAttribute("divisions", divisions);

        String divIdStr = request.getParameter("divisionId");
        String fromStr  = request.getParameter("fromDate");
        String toStr    = request.getParameter("toDate");

        if (divIdStr != null && !divIdStr.isBlank()
                && fromStr != null && !fromStr.isBlank()
                && toStr != null && !toStr.isBlank()) {
            try {
                int divisionId = Integer.parseInt(divIdStr);
                Date from = Date.valueOf(fromStr);
                Date to   = Date.valueOf(toStr);
                request.setAttribute("rows", dao.agendaByDivision(divisionId, from, to));
                request.setAttribute("divisionId", divIdStr);
                request.setAttribute("fromDate", fromStr);
                request.setAttribute("toDate", toStr);
            } catch (IllegalArgumentException ex) {
                request.setAttribute("ERROR", "Khoảng ngày không hợp lệ.");
            }
        }
        request.getRequestDispatcher("/agenda.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException { doGet(req, resp); }
}
