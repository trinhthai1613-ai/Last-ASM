package com.leavemgmt.filter;

import jakarta.servlet.*;
import java.io.IOException;

public class EncodingFilter implements Filter {
    @Override public void init(FilterConfig filterConfig) {}
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");
        chain.doFilter(req, res);
    }
    @Override public void destroy() {}
}
