package com.leavemgmt.servlet;

import com.leavemgmt.dao.RequestDAO;
import com.leavemgmt.model.LeaveType;
import com.leavemgmt.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "RequestCreateServlet", urlPatterns = {"/app/request/create"})
public class RequestCreateServlet extends HttpServlet {

    private boolean blockIfTopLevel(User u, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (u != null && u.isTopLevel()) {
            request.getSession().setAttribute("FLASH_MSG",
                    "Bạn là cấp cao nhất, không cần tạo đơn nghỉ.");
            response.sendRedirect(request.getContextPath() + "/app/home");
            return true;
        }
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User u = (User) request.getSession().getAttribute("LOGIN_USER");
        if (blockIfTopLevel(u, request, response)) return;

        RequestDAO dao = new RequestDAO();
        List<LeaveType> types = dao.getLeaveTypes();
        request.setAttribute("types", types);

        String typeIdStr = request.getParameter("typeId");
        if (typeIdStr != null && !typeIdStr.isBlank()) {
            try {
                int typeId = Integer.parseInt(typeIdStr);
                request.setAttribute("typeId", typeIdStr);

                LeaveType lt = dao.getLeaveType(typeId);
                if (lt != null && !"OTHER".equalsIgnoreCase(lt.getTypeCode())) {
                    request.setAttribute("reasons", dao.getReasonOptionsNames(typeId));
                }
            } catch (NumberFormatException ignore) {
                request.setAttribute("typeId", null);
            }
        }
        request.getRequestDispatcher("/request_create.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User u = (User) request.getSession().getAttribute("LOGIN_USER");
        if (blockIfTopLevel(u, request, response)) return;

        RequestDAO dao = new RequestDAO();

        String leaveTypeIdStr = request.getParameter("leaveTypeId");
        if (leaveTypeIdStr == null || leaveTypeIdStr.isBlank()) {
            request.setAttribute("ERROR", "Vui lòng chọn Leave Type trước khi gửi.");
            request.setAttribute("types", dao.getLeaveTypes());
            request.getRequestDispatcher("/request_create.jsp").forward(request, response);
            return;
        }
        int leaveTypeId;
        try { leaveTypeId = Integer.parseInt(leaveTypeIdStr); }
        catch (NumberFormatException e) {
            request.setAttribute("ERROR", "Loại nghỉ không hợp lệ.");
            request.setAttribute("types", dao.getLeaveTypes());
            request.getRequestDispatcher("/request_create.jsp").forward(request, response);
            return;
        }

        LeaveType lt = dao.getLeaveType(leaveTypeId);
        boolean isOther = lt != null && "OTHER".equalsIgnoreCase(lt.getTypeCode());

        String fromStr = request.getParameter("fromDate");
        String toStr   = request.getParameter("toDate");
        if (fromStr == null || fromStr.isBlank() || toStr == null || toStr.isBlank()) {
            request.setAttribute("ERROR", "From/To là bắt buộc.");
            request.setAttribute("types", dao.getLeaveTypes());
            request.setAttribute("typeId", leaveTypeIdStr);
            if (!isOther) request.setAttribute("reasons", dao.getReasonOptionsNames(leaveTypeId));
            request.getRequestDispatcher("/request_create.jsp").forward(request, response);
            return;
        }

        Date fromDate, toDate;
        try { fromDate = Date.valueOf(fromStr); toDate = Date.valueOf(toStr); }
        catch (IllegalArgumentException e) {
            request.setAttribute("ERROR", "Định dạng ngày không hợp lệ (yyyy-MM-dd).");
            request.setAttribute("types", dao.getLeaveTypes());
            request.setAttribute("typeId", leaveTypeIdStr);
            if (!isOther) request.setAttribute("reasons", dao.getReasonOptionsNames(leaveTypeId));
            request.getRequestDispatcher("/request_create.jsp").forward(request, response);
            return;
        }

        LocalDate today = LocalDate.now();
        if (fromDate.toLocalDate().isBefore(today)) {
            request.setAttribute("ERROR", "Ngày bắt đầu không được nhỏ hơn hôm nay.");
            request.setAttribute("types", dao.getLeaveTypes());
            request.setAttribute("typeId", leaveTypeIdStr);
            if (!isOther) request.setAttribute("reasons", dao.getReasonOptionsNames(leaveTypeId));
            request.getRequestDispatcher("/request_create.jsp").forward(request, response);
            return;
        }
        if (toDate.before(fromDate)) {
            request.setAttribute("ERROR", "Ngày kết thúc phải ≥ ngày bắt đầu.");
            request.setAttribute("types", dao.getLeaveTypes());
            request.setAttribute("typeId", leaveTypeIdStr);
            if (!isOther) request.setAttribute("reasons", dao.getReasonOptionsNames(leaveTypeId));
            request.getRequestDispatcher("/request_create.jsp").forward(request, response);
            return;
        }

        String reason = request.getParameter("reason");
        Integer reasonOptionId = null;
        if (isOther) {
            if (reason == null || reason.isBlank()) {
                request.setAttribute("ERROR", "Vui lòng nhập Reason khi chọn loại 'Khác'.");
                request.setAttribute("types", dao.getLeaveTypes());
                request.setAttribute("typeId", leaveTypeIdStr);
                request.getRequestDispatcher("/request_create.jsp").forward(request, response);
                return;
            }
        } else {
            String roid = request.getParameter("reasonOptionId");
            if (roid != null && !roid.isBlank()) {
                try { reasonOptionId = Integer.valueOf(roid); } catch (NumberFormatException ignore) {}
            }
        }

        int requestId = dao.createRequest(u.getUserId(), leaveTypeId, reasonOptionId, fromDate, toDate, reason);

        request.setAttribute("MSG", "Created request ID = " + requestId);
        request.setAttribute("types", dao.getLeaveTypes());
        request.setAttribute("typeId", String.valueOf(leaveTypeId));
        if (!isOther) request.setAttribute("reasons", dao.getReasonOptionsNames(leaveTypeId));
        request.getRequestDispatcher("/request_create.jsp").forward(request, response);
    }
}
