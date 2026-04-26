package com.courseportal;

import java.io.IOException;
import java.sql.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/courseportal";
    private static final String DB_USER = "portaluser";
    private static final String DB_PASSWORD = "portalpass";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean validUser = false;
        String userType = null;
        int userId = -1;
        String fullName = null;

        String studentName = request.getParameter("stu_name");
        String studentPassword = request.getParameter("stu_password");

        String professorName = request.getParameter("prof_name");
        String professorPassword = request.getParameter("prof_password");

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
                if (studentName != null && studentPassword != null) {
                    userType = "student";
                    String sql = "SELECT user_id, full_name FROM users WHERE username = ? AND password = ? AND role = 'student'";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, studentName);
                    ps.setString(2, studentPassword);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        validUser = true;
                        userId = rs.getInt("user_id");
                        fullName = rs.getString("full_name");
                    }

                } else if (professorName != null && professorPassword != null) {
                    userType = "professor";
                    String sql = "SELECT user_id, full_name FROM users WHERE username = ? AND password = ? AND role = 'professor'";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, professorName);
                    ps.setString(2, professorPassword);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        validUser = true;
                        userId = rs.getInt("user_id");
                        fullName = rs.getString("full_name");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (validUser) {
            
            HttpSession session = request.getSession();
            session.setAttribute("userId", userId);
            session.setAttribute("fullName", fullName);
            session.setAttribute("role", userType);

            if ("student".equals(userType)) {
                session.setAttribute("studentId", userId);
                session.setAttribute("studentName", fullName);
                response.sendRedirect("student-dash.html");
            } else {
                session.setAttribute("professorId", userId);
                session.setAttribute("professorName", fullName);
                response.sendRedirect("professor-dash.html");
            }
        } else {
            response.setContentType("text/plain");
            String errorMessage = "Invalid Credentials. ";
            if ("student".equals(userType)) {
                errorMessage += "Student username or password incorrect.";
            } else if ("professor".equals(userType)) {
                errorMessage += "Professor username or password incorrect.";
            } else {
                errorMessage += "Missing login parameters.";
            }
            response.getWriter().write(errorMessage);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        request.getRequestDispatcher("index.html").forward(request, response);
    }
}
