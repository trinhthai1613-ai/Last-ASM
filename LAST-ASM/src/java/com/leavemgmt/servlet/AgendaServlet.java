package com.leavemgmt.servlet;

import com.leavemgmt.dao.AgendaDAO;
import com.leavemgmt.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Agenda ma trận theo tuần (xanh=đi làm/chờ duyệt, đỏ=đã duyệt nghỉ, Chủ nhật xám).
 * Chỉ NHÂN SỰ CẤP CAO NHẤT (division gốc) mới được xem.
 * Ẩn chính người xem khỏi danh sách nhân sự.
 */
@WebServlet(name = "AgendaServlet", urlPatterns = {"/app/agenda"})
public class AgendaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User u = (User) req.getSession().getAttribute("LOGIN_USER");
        if (u == null) { resp.sendRedirect(req.getContextPath()+"/login"); return; }

        AgendaDAO dao = new AgendaDAO();

        // Quyền: chỉ cấp cao nhất mới xem Agenda
        if (!dao.isTopMostUser(u.getUserId())) {
            req.getSession().setAttribute("FLASH_MSG", "Chỉ nhân sự cấp cao nhất mới được xem Agenda.");
            resp.sendRedirect(req.getContextPath()+"/app/home");
            return;
        }

        // Khoảng ngày gốc
        LocalDate today = LocalDate.now();
        LocalDate start = parseLD(req.getParameter("start"), today);
        LocalDate end   = parseLD(req.getParameter("end")  , start.plusDays(27)); // mặc định 4 tuần
        if (end.isBefore(start)) end = start;

        // Tuần hiện tại (0-based)
        int weekParam = parseInt(req.getParameter("week"), 0);
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1; // inclusive
        int totalWeeks = (int)Math.ceil(totalDays / 7.0);
        if (totalWeeks <= 0) totalWeeks = 1;
        if (weekParam < 0) weekParam = 0;
        if (weekParam > totalWeeks - 1) weekParam = totalWeeks - 1;

        // Cửa sổ tuần
        LocalDate pageStart = start.plusDays(weekParam * 7L);
        LocalDate pageEnd   = pageStart.plusDays(6);
        if (pageEnd.isAfter(end)) pageEnd = end;

        boolean hasPrev = weekParam > 0;
        boolean hasNext = (weekParam < totalWeeks - 1);

        // Các ngày của tuần
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = pageStart; !d.isAfter(pageEnd); d = d.plusDays(1)) days.add(d);

        // Thành viên (ẩn chính người xem)
        Map<Integer, String> members = dao.loadTeamMembers(u.getUserId());
        members.remove(u.getUserId());

        // Grid mặc định (work)
        Map<Integer, Map<LocalDate, String>> grid = new LinkedHashMap<>();
        for (Integer uid : members.keySet()) {
            Map<LocalDate, String> row = new HashMap<>();
            for (LocalDate d : days) row.put(d, "work");
            grid.put(uid, row);
        }

        // Đơn chồng lấn trong tuần → chỉ tô đỏ khi approved
        List<AgendaDAO.LeaveSpan> leaves =
                dao.loadLeavesForTeam(u.getUserId(), Date.valueOf(pageStart), Date.valueOf(pageEnd));

        for (AgendaDAO.LeaveSpan lv : leaves) {
            Map<LocalDate, String> row = grid.get(lv.userId);
            if (row == null) continue;

            LocalDate fromLd = lv.from.toLocalDate();
            LocalDate toLd   = lv.to.toLocalDate();

            LocalDate s = fromLd.isBefore(pageStart) ? pageStart : fromLd;
            LocalDate e = toLd.isAfter(pageEnd) ? pageEnd : toLd;

            for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                if ("approved".equals(lv.normStatus)) {
                    row.put(d, "leave");   // đỏ khi đã duyệt
                }
            }
        }

        // Xuất cho JSP
        req.setAttribute("start", start);
        req.setAttribute("end", end);
        req.setAttribute("week", weekParam);
        req.setAttribute("hasPrev", hasPrev);
        req.setAttribute("hasNext", hasNext);
        req.setAttribute("pageStart", pageStart);
        req.setAttribute("pageEnd", pageEnd);

        req.setAttribute("days", days);
        req.setAttribute("members", members);
        req.setAttribute("grid", grid);

        req.getRequestDispatcher("/agenda.jsp").forward(req, resp);
    }

    private LocalDate parseLD(String s, LocalDate defVal) {
        try { return (s == null || s.isBlank()) ? defVal : LocalDate.parse(s); }
        catch (Exception ignore) { return defVal; }
    }
    private int parseInt(String s, int defVal) {
        try { return (s == null || s.isBlank()) ? defVal : Integer.parseInt(s); }
        catch (Exception ignore) { return defVal; }
    }
}
