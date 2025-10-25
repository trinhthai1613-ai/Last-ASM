package com.leavemgmt.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class AuthFilter implements Filter {
    @Override public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Ngăn cache trang bảo vệ
        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        res.setHeader("Pragma", "no-cache");
        res.setDateHeader("Expires", 0);

        HttpSession session = req.getSession(false);
        Object user = (session == null) ? null : session.getAttribute("LOGIN_USER");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        chain.doFilter(request, response);
    }

    @Override public void destroy() {}
}
