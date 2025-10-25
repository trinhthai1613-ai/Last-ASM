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
import java.util.*;

/**
 * Agenda ma trận: hàng = nhân sự; cột = từng ngày trong khoảng.
 * work (xanh), leave (đỏ/đã duyệt), pending (vàng/đang chờ).
 * Chỉ người quản lý (không phải leaf) mới xem được.
 */
@WebServlet(name = "AgendaServlet", urlPatterns = {"/app/agenda"})
public class AgendaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User u = (User) req.getSession().getAttribute("LOGIN_USER");
        if (u == null) { resp.sendRedirect(req.getContextPath()+"/login"); return; }
        if (u.isLeaf()) {
            req.getSession().setAttribute("FLASH_MSG", "Bạn không có quyền xem Agenda.");
            resp.sendRedirect(req.getContextPath()+"/app/home");
            return;
        }

        // Khoảng ngày
        LocalDate today = LocalDate.now();
        LocalDate start = parseLD(req.getParameter("start"), today);
        LocalDate end   = parseLD(req.getParameter("end")  , start.plusDays(9)); // mặc định 10 ngày
        if (end.isBefore(start)) end = start;

        // Danh sách ngày
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) days.add(d);

        AgendaDAO dao = new AgendaDAO();

        // Thành viên thuộc subtree của manager
        Map<Integer, String> members = dao.loadTeamMembers(u.getUserId());

        // Khởi tạo grid mặc định (work)
        Map<Integer, Map<LocalDate, String>> grid = new LinkedHashMap<>();
        for (Integer uid : members.keySet()) {
            Map<LocalDate, String> row = new HashMap<>();
            for (LocalDate d : days) row.put(d, "work");
            grid.put(uid, row);
        }

        // Lấy các khoảng nghỉ chồng lấn với [start..end]
        List<AgendaDAO.LeaveSpan> leaves =
                dao.loadLeavesForTeam(u.getUserId(), Date.valueOf(start), Date.valueOf(end));

        for (AgendaDAO.LeaveSpan lv : leaves) {
            Map<LocalDate, String> row = grid.get(lv.userId);
            if (row == null) continue;

            // lv.from/lv.to là java.sql.Date -> đổi sang LocalDate
            LocalDate fromLd = lv.from.toLocalDate();
            LocalDate toLd   = lv.to.toLocalDate();

            LocalDate s = fromLd.isBefore(start) ? start : fromLd;
            LocalDate e = toLd.isAfter(end) ? end : toLd;

            for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                if ("approved".equals(lv.normStatus)) {
                    row.put(d, "leave");                 // đỏ
                } else if ("pending".equals(lv.normStatus)) {
                    if (!"leave".equals(row.get(d))) {   // chưa bị đỏ
                        row.put(d, "pending");           // vàng
                    }
                }
            }
        }

        // Đẩy dữ liệu ra JSP
        req.setAttribute("start", start);
        req.setAttribute("end", end);
        req.setAttribute("days", days);            // List<LocalDate>
        req.setAttribute("members", members);      // Map<userId, fullName>
        req.setAttribute("grid", grid);            // Map<userId, Map<LocalDate, status>>

        req.getRequestDispatcher("/agenda.jsp").forward(req, resp);
    }

    private LocalDate parseLD(String s, LocalDate defVal) {
        try { return (s == null || s.isBlank()) ? defVal : LocalDate.parse(s); }
        catch (Exception ignore) { return defVal; }
    }
}
