package com.courseportal;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    private static final String ADMIN_PASSWORD = "22152210124";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String submittedPassword = request.getParameter("adminPassword");

        if (ADMIN_PASSWORD.equals(submittedPassword)) {
            response.sendRedirect("html/admin-dash.html");
        } else {
            response.setContentType("text/plain");
            response.getWriter().write("Invalid Admin Password.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("admin-login.html").forward(request, response);
    }
}
