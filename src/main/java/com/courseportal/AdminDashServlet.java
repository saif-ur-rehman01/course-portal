package com.courseportal;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet(urlPatterns = {
    "/add-course", "/delete-course", "/view-courses",
    "/add-users", "/delete-users", "/view-users", "/manage-users", "/remove-grade"
})
public class AdminDashServlet extends HttpServlet {
    private Connection conn;

    public void init() throws ServletException {
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/courseportal",
                "portaluser",
                "portalpass"
            );
        } catch (Exception e) {
            throw new ServletException("Database connection failed", e);
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        String path = req.getServletPath();

        switch (path) {
            case "/add-course": addCourse(req, res); break;
            case "/delete-course": deleteCourse(req, res); break;
            case "/view-courses": viewCourses(req, res); break;
            case "/add-users": addUser(req, res); break;
            case "/delete-users": deleteUser(req, res); break;
            case "/view-users": viewUsers(req, res); break;
            case "/remove-grade": removeGrade(req, res); break;
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if ("/manage-users".equals(req.getServletPath())) {
            listUsers(res);
        }
    }

    

    private void addCourse(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String code = req.getParameter("code").trim();
        String title = req.getParameter("title").trim();
        int credits = Integer.parseInt(req.getParameter("credits"));
        int deptId = Integer.parseInt(req.getParameter("dept_id"));
        String professorIdStr = req.getParameter("professor_id");
        Integer professorId = (professorIdStr != null && !professorIdStr.isEmpty()) ? Integer.parseInt(professorIdStr) : null;

        String sql = "INSERT INTO courses (code, title, credits, dept_id, professor_id) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            stmt.setString(2, title);
            stmt.setInt(3, credits);
            stmt.setInt(4, deptId);
            if (professorId != null) stmt.setInt(5, professorId);
            else stmt.setNull(5, java.sql.Types.INTEGER);

            int rows = stmt.executeUpdate();
            JSONObject json = new JSONObject();
            json.put("success", rows > 0);
            json.put("message", "Course added successfully");
            res.getWriter().print(json.toString());
        } catch (SQLException e) {
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("error", e.getMessage());
            res.getWriter().print(error.toString());
        }
    }

    private void deleteCourse(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String code = req.getParameter("code");
        String sql = "DELETE FROM courses WHERE code = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            int rows = stmt.executeUpdate();
            JSONObject json = new JSONObject();
            json.put("success", rows > 0);
            json.put("message", rows > 0 ? "Course deleted successfully" : "Course not found");
            res.getWriter().print(json.toString());
        } catch (SQLException e) {
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("error", e.getMessage());
            res.getWriter().print(error.toString());
        }
    }

    private void viewCourses(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String code = req.getParameter("code");
        String deptIdStr = req.getParameter("dept_id");

        StringBuilder sql = new StringBuilder(
            "SELECT c.course_id, c.code, c.title, c.credits, c.dept_id, c.professor_id, " +
            "d.name AS department_name, u.full_name AS professor_name " +
            "FROM courses c " +
            "JOIN departments d ON c.dept_id = d.dept_id " +
            "LEFT JOIN users u ON c.professor_id = u.user_id WHERE 1=1"
        );

        if (code != null && !code.trim().isEmpty()) {
            sql.append(" AND c.code = ?");
        }
        if (deptIdStr != null && !deptIdStr.trim().isEmpty()) {
            sql.append(" AND c.dept_id = ?");
        }

        JSONArray courses = new JSONArray();

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (code != null && !code.trim().isEmpty()) {
                stmt.setString(idx++, code.trim());
            }
            if (deptIdStr != null && !deptIdStr.trim().isEmpty()) {
                stmt.setInt(idx++, Integer.parseInt(deptIdStr.trim()));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject course = new JSONObject();
                    course.put("course_id", rs.getInt("course_id"));
                    course.put("code", rs.getString("code"));
                    course.put("title", rs.getString("title"));
                    course.put("credits", rs.getInt("credits"));
                    course.put("dept_id", rs.getInt("dept_id"));
                    course.put("department_name", rs.getString("department_name"));
                    course.put("professor_id", rs.getObject("professor_id"));
                    course.put("professor_name", rs.getString("professor_name"));
                    courses.put(course);
                }
            }
        } catch (SQLException e) {
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("error", e.getMessage());
            res.getWriter().print(error.toString());
            return;
        }

        JSONObject json = new JSONObject();
        json.put("success", true);
        json.put("courses", courses);
        res.getWriter().print(json.toString());
    }



    private void addUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String username = req.getParameter("username").trim().toLowerCase();
        String password = req.getParameter("password");
        String role = req.getParameter("role");
        String fullName = req.getParameter("full_name");
        String email = req.getParameter("email");

        String sql = "INSERT INTO users (username, password, role, full_name, email) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, fullName);
            ps.setString(5, email);
            ps.executeUpdate();
            res.getWriter().write("User added successfully");
        } catch (SQLException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("Error: " + e.getMessage());
        }
    }

    private void deleteUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String username = req.getParameter("username");
        String sql = "DELETE FROM users WHERE username = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            int rowsAffected = ps.executeUpdate();
            res.getWriter().write(rowsAffected > 0 ? "User deleted successfully" : "User not found");
        } catch (SQLException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("Error: " + e.getMessage());
        }
    }

    private void listUsers(HttpServletResponse res) throws IOException {
        String sql = "SELECT user_id, username, full_name, email, role FROM users ORDER BY user_id";
        JSONArray users = new JSONArray();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                JSONObject user = new JSONObject();
                user.put("user_id", rs.getInt("user_id"));
                user.put("username", rs.getString("username"));
                user.put("full_name", rs.getString("full_name"));
                user.put("email", rs.getString("email"));
                user.put("role", rs.getString("role"));
                users.put(user);
            }
            res.setContentType("application/json");
            res.getWriter().write(users.toString());
        } catch (SQLException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("[]");
        }
    }

    private void viewUsers(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String role = req.getParameter("role");
        String username = req.getParameter("username");

        StringBuilder sql = new StringBuilder("SELECT user_id, username, full_name, email, role FROM users WHERE 1=1");
        if (role != null && !role.isEmpty()) sql.append(" AND role = ?");
        if (username != null && !username.isEmpty()) sql.append(" AND username = ?");
        sql.append(" ORDER BY user_id");

        JSONArray users = new JSONArray();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (role != null && !role.isEmpty()) ps.setString(idx++, role);
            if (username != null && !username.isEmpty()) ps.setString(idx++, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JSONObject user = new JSONObject();
                user.put("user_id", rs.getInt("user_id"));
                user.put("username", rs.getString("username"));
                user.put("full_name", rs.getString("full_name"));
                user.put("email", rs.getString("email"));
                user.put("role", rs.getString("role"));
                users.put(user);
            }
            res.setContentType("application/json");
            res.getWriter().write(users.toString());
        } catch (SQLException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("[]");
        }
    }

    private void removeGrade(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String username = req.getParameter("username");
        String code = req.getParameter("code");

        if (username == null || code == null) {
            res.getWriter().write("Missing input.");
            return;
        }

        try {
            // 1. Get student_id
            int studentId = -1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE username = ? AND role = 'student'")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) studentId = rs.getInt("user_id");
                else {
                    res.getWriter().write("Student not found.");
                    return;
                }
            }
            // 2. Get course_id
            int courseId = -1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT course_id FROM courses WHERE code = ?")) {
                ps.setString(1, code);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) courseId = rs.getInt("course_id");
                else {
                    res.getWriter().write("Course not found.");
                    return;
                }
            }
            // 3. Remove grade in registrations
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE registrations SET grade = NULL WHERE student_id = ? AND course_id = ?")) {
                ps.setInt(1, studentId);
                ps.setInt(2, courseId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    res.getWriter().write("Grade removed successfully.");
                } else {
                    res.getWriter().write("Registration not found for this student and course.");
                }
            }
        } catch (SQLException e) {
            res.getWriter().write("Database error: " + e.getMessage());
        }
    }
    
    @Override
    public void destroy() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException ignored) {}
    }
}

