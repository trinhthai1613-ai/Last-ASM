package com.leavemgmt.servlet;

import com.leavemgmt.dao.UserDAO;
import com.leavemgmt.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet(name="LoginServlet", urlPatterns={"/login"})
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession s = request.getSession(false);
        if (s != null && s.getAttribute("LOGIN_USER") != null) {
            response.sendRedirect(request.getContextPath() + "/app/home");
            return;
        }
        response.setHeader("Cache-Control","no-cache, no-store, must-revalidate");
        response.setHeader("Pragma","no-cache");
        response.setDateHeader("Expires", 0);
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            request.setAttribute("ERROR", "Vui lòng nhập username và password.");
            doGet(request, response);
            return;
        }

        User user = new UserDAO().login(username, password); // plain-compare
        if (user == null) {
            request.setAttribute("ERROR", "Sai username hoặc password.");
            doGet(request, response);
            return;
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("LOGIN_USER", user);
        response.sendRedirect(request.getContextPath() + "/app/home");
    }
}
