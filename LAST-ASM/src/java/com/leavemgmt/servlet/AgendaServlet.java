package com.leavemgmt.servlet;

import com.leavemgmt.dao.AgendaDAO;
import com.leavemgmt.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@WebServlet(name="AgendaServlet", urlPatterns={"/app/agenda"})
public class AgendaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User u = (User) req.getSession().getAttribute("LOGIN_USER");
        if (u == null) { resp.sendRedirect(req.getContextPath()+"/login"); return; }
        // Nhân viên cấp thấp nhất không được xem agenda
        if (u.isLeaf()) {
            req.getSession().setAttribute("FLASH_MSG", "Bạn không có quyền xem Agenda.");
            resp.sendRedirect(req.getContextPath()+"/app/home");
            return;
        }

        // Lấy khoảng ngày
        LocalDate today = LocalDate.now();
        LocalDate start = parse(req.getParameter("start"), today);
        LocalDate end   = parse(req.getParameter("end"), start.plusDays(9)); // mặc định 10 ngày

        if (end.isBefore(start)) end = start;

        // Tạo danh sách ngày
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) days.add(d);

        AgendaDAO dao = new AgendaDAO();

        // Danh sách nhân sự trong subtree của quản lý
        Map<Integer, String> members = dao.loadTeamMembers(u.getUserId());

        // Khởi tạo grid trạng thái mặc định (work)
        Map<Integer, Map<LocalDate, String>> grid = new LinkedHashMap<>();
        for (Integer uid : members.keySet()) {
            Map<LocalDate, String> row = new HashMap<>();
            for (LocalDate d : days) row.put(d, "work");
            grid.put(uid, row);
        }

        // Lấy các đơn nghỉ trùng khoảng
        dao.loadLeavesForTeam(u.getUserId(), Date.valueOf(start), Date.valueOf(end))
           .forEach(lv -> {
               Map<LocalDate, String> row = grid.get(lv.userId);
               if (row == null) return;
               LocalDate s = lv.from.isBefore(start) ? start : lv.from.toLocalDate();
               LocalDate e = lv.to.isAfter(end) ? end : lv.to.toLocalDate();

               for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                   if ("approved".equals(lv.normStatus)) {
                       row.put(d, "leave");      // đỏ
                   } else if ("pending".equals(lv.normStatus)) {
                       // chỉ tô vàng nếu ô chưa bị đánh đỏ
                       if (!"leave".equals(row.get(d))) row.put(d, "pending");
                   }
               }
           });

        // Đẩy dữ liệu cho JSP
        req.setAttribute("start", start);
        req.setAttribute("end", end);
        req.setAttribute("days", days);                // List<LocalDate>
        req.setAttribute("members", members);          // Map<userId, fullName>
        req.setAttribute("grid", grid);                // Map<userId, Map<LocalDate, "work|leave|pending">>

        req.getRequestDispatcher("/agenda.jsp").forward(req, resp);
    }

    private LocalDate parse(String s, LocalDate def) {
        try { return (s==null||s.isBlank()) ? def : LocalDate.parse(s); }
        catch (Exception e) { return def; }
    }
}
