package com.courseportal;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/ProfessorDashServlet")
public class ProfessorDashServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/courseportal";
    private static final String DB_USER = "portaluser";
    private static final String DB_PASSWORD = "portalpass";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json; charset=UTF-8");
        String action = req.getParameter("action");
        HttpSession session = req.getSession(false);
        Object pid = (session != null) ? session.getAttribute("professorId") : null;
        int professorId = (pid instanceof Integer) ? (Integer) pid : -1;

        try (PrintWriter out = res.getWriter()) {
            switch (action == null ? "" : action) {
                case "session":
                    String name = (String) session.getAttribute("fullName");
                    JSONObject obj = new JSONObject();
                    obj.put("name", name == null ? JSONObject.NULL : name);
                    out.print(obj.toString());
                    break;
                case "catalog":
                    out.print(getProfessorCourses(professorId).toString());
                    break;
                case "students":
                    out.print(getProfessorStudents(professorId).toString());
                    break;
                case "timetable":
                    out.print(getProfessorTimetable(professorId).toString());
                    break;
                default:
                    out.print("[]");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String action = req.getParameter("action");
        if ("uploadGrade".equals(action)) {
            handleUploadGrade(req, res);
        }
    }

    private JSONArray getProfessorCourses(int professorId) {
        JSONArray arr = new JSONArray();
        String sql = "SELECT c.code, c.title, c.credits, c.semester_num, d.name AS department " +
                     "FROM courses c JOIN departments d ON c.dept_id = d.dept_id " +
                     "WHERE c.professor_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, professorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JSONObject o = new JSONObject();
                o.put("code", rs.getString("code"));
                o.put("title", rs.getString("title"));
                o.put("credits", rs.getInt("credits"));
                o.put("semester_num", rs.getInt("semester_num"));
                o.put("department", rs.getString("department"));
                arr.put(o);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return arr;
    }

    private JSONArray getProfessorStudents(int professorId) {
        JSONArray arr = new JSONArray();
        String sql = "SELECT u.user_id AS student_id, u.full_name, u.username, u.email, c.code AS course_code " +
                    "FROM registrations r " +
                    "JOIN users u ON r.student_id = u.user_id " +
                    "JOIN courses c ON r.course_id = c.course_id " +
                    "WHERE c.professor_id = ? AND r.grade IS NULL";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, professorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JSONObject o = new JSONObject();
                o.put("student_id", rs.getInt("student_id"));
                o.put("full_name", rs.getString("full_name"));
                o.put("username", rs.getString("username"));
                o.put("email", rs.getString("email"));
                o.put("course_code", rs.getString("course_code"));
                arr.put(o);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return arr;
    }

    private JSONArray getProfessorTimetable(int professorId) {
        JSONArray arr = new JSONArray();
        String sql = "SELECT c.code AS course_code, cs.day, cs.start_time, cs.end_time " +
                    "FROM courses c " +
                    "JOIN registrations r ON c.course_id = r.course_id " +
                    "JOIN course_schedule cs ON c.course_id = cs.course_id " +
                    "WHERE c.professor_id = ? AND r.grade IS NULL " +
                    "ORDER BY " +
                    "CASE cs.day " +
                    "    WHEN 'Monday' THEN 1 " +
                    "    WHEN 'Tuesday' THEN 2 " +
                    "    WHEN 'Wednesday' THEN 3 " +
                    "    WHEN 'Thursday' THEN 4 " +
                    "    WHEN 'Friday' THEN 5 " +
                    "    WHEN 'Saturday' THEN 6 " +
                    "    WHEN 'Sunday' THEN 7 " +
                    "    ELSE 8 END, " +
                    "cs.start_time";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, professorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JSONObject o = new JSONObject();
                o.put("course_code", rs.getString("course_code"));
                o.put("day", rs.getString("day"));
                o.put("start_time", rs.getString("start_time"));
                o.put("end_time", rs.getString("end_time"));
                arr.put(o);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return arr;
    }

    private void handleUploadGrade(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String username = req.getParameter("username");
        String code = req.getParameter("code");
        String grade = req.getParameter("grade");
        JSONObject result = new JSONObject();
        if (username == null || code == null || grade == null) {
            result.put("error", "Missing input.");
            res.getWriter().print(result.toString());
            return;
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            
            int studentId = -1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE username = ? AND role = 'student'")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) studentId = rs.getInt("user_id");
                else {
                    result.put("error", "Student not found.");
                    res.getWriter().print(result.toString());
                    return;
                }
            }
           
            int courseId = -1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT course_id FROM courses WHERE code = ? AND professor_id = ?")) {
                ps.setString(1, code);
                ps.setInt(2, ((Integer)req.getSession(false).getAttribute("professorId")));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) courseId = rs.getInt("course_id");
                else {
                    result.put("error", "Course not found or not taught by you.");
                    res.getWriter().print(result.toString());
                    return;
                }
            }
            
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE registrations SET grade = ? WHERE student_id = ? AND course_id = ?")) {
                ps.setString(1, grade.toUpperCase());
                ps.setInt(2, studentId);
                ps.setInt(3, courseId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    result.put("message", "Grade uploaded successfully.");
                } else {
                    result.put("error", "Registration not found for this student and course.");
                }
                res.getWriter().print(result.toString());
            }
        } catch (SQLException e) {
            result.put("error", "Database error: " + e.getMessage());
            res.getWriter().print(result.toString());
        }
    }
}