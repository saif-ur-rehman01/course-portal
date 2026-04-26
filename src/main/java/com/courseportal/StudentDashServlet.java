package com.courseportal;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

@WebServlet("/StudentDashServlet")
public class StudentDashServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/courseportal";
    private static final String DB_USER = "portaluser";
    private static final String DB_PASSWORD = "portalpass";

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new ServletException("Failed to load JDBC Driver", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json; charset=UTF-8");
        String action = req.getParameter("action");
        HttpSession session = req.getSession(false);

        int studentId = -1;
        String studentName = null;
        if (session != null && session.getAttribute("studentId") != null) {
            studentId = (int) session.getAttribute("studentId");
            studentName = (String) session.getAttribute("studentName");
        }

        try (PrintWriter out = res.getWriter()) {
            switch (action == null ? "" : action) {
                case "session":
                    JSONObject sessionObj = new JSONObject();
                    if (studentId < 0) {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        sessionObj.put("error", "Unauthorized. Please log in.");
                    } else {
                        sessionObj.put("name", studentName == null ? JSONObject.NULL : studentName);
                    }
                    out.print(sessionObj.toString());
                    break;
                case "enrolled":
                    out.print(handleEnrolled(studentId).toString());
                    break;
                case "results":
                    out.print(handleResults(studentId).toString());
                    break;
                case "dropList":
                    out.print(handleDropList(studentId).toString());
                    break;
                default:
                    out.print(handleCatalog(req).toString());
                    break;
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json; charset=UTF-8");
        String action = req.getParameter("action");
        HttpSession session = req.getSession(false);

        int studentId = -1;
        if (session != null && session.getAttribute("studentId") != null) {
            studentId = (int) session.getAttribute("studentId");
        }

        JSONObject result = new JSONObject();
        if (studentId < 0) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            result.put("error", "Unauthorized. Please log in.");
            res.getWriter().print(result.toString());
            return;
        }

        switch (action == null ? "" : action) {
            case "registerByCode":
                handleRegisterByCode(req, res, studentId);
                break;
            case "drop":
                handleDropCourse(req, res, studentId);
                break;
            default:
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                result.put("error", "Invalid action provided for POST request.");
                res.getWriter().print(result.toString());
        }
    }


    private JSONArray handleEnrolled(int studentId) {
        JSONArray arr = new JSONArray();
        if (studentId < 0) return arr;
        String sql = """
            SELECT c.course_id, c.code, c.title, c.credits, d.name AS department,
            u.full_name AS professor, cs.day, cs.start_time, cs.end_time
            FROM registrations r
            JOIN courses c ON r.course_id = c.course_id
            JOIN departments d ON c.dept_id = d.dept_id
            LEFT JOIN users u ON c.professor_id = u.user_id
            LEFT JOIN course_schedule cs ON c.course_id = cs.course_id
            WHERE r.student_id = ? AND LOWER(r.status) = 'registered' AND r.grade IS NULL
            ORDER BY 
                CASE cs.day
                    WHEN 'Monday' THEN 1
                    WHEN 'Tuesday' THEN 2
                    WHEN 'Wednesday' THEN 3
                    WHEN 'Thursday' THEN 4
                    WHEN 'Friday' THEN 5
                    WHEN 'Saturday' THEN 6
                    WHEN 'Sunday' THEN 7
                    ELSE 8
                END,
                cs.start_time
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                Map<Integer, JSONObject> courseMap = new LinkedHashMap<>();
                while (rs.next()) {
                    int courseId = rs.getInt("course_id");
                    JSONObject course = courseMap.get(courseId);
                    if (course == null) {
                        course = new JSONObject();
                        course.put("code", rs.getString("code"));
                        course.put("title", rs.getString("title"));
                        course.put("credits", rs.getInt("credits"));
                        course.put("department", rs.getString("department"));
                        course.put("professor", rs.getString("professor") != null ? rs.getString("professor") : "TBD");
                        course.put("schedules", new JSONArray());
                        courseMap.put(courseId, course);
                    }
                    String day = rs.getString("day");
                    Time startTime = rs.getTime("start_time");
                    Time endTime = rs.getTime("end_time");
                    if (day != null && startTime != null && endTime != null) {
                        JSONObject schedule = new JSONObject();
                        schedule.put("day", day);
                        schedule.put("start_time", startTime.toString());
                        schedule.put("end_time", endTime.toString());
                        course.getJSONArray("schedules").put(schedule);
                    }
                }
                arr = new JSONArray(courseMap.values());
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return arr;
    }

    private JSONArray handleResults(int studentId) {
        JSONArray arr = new JSONArray();
        if (studentId < 0) return arr;
        String sql = 
            "SELECT c.code, c.title, c.credits, d.name AS department, " +
            "u.full_name AS professor, r.grade " +
            "FROM registrations r " +
            "JOIN courses c ON r.course_id = c.course_id " +
            "JOIN departments d ON c.dept_id = d.dept_id " +
            "LEFT JOIN users u ON c.professor_id = u.user_id " +
            "WHERE r.student_id = ? " +
            "AND LOWER(r.status) = 'registered' " +
            "AND r.grade IS NOT NULL";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject o = new JSONObject();
                    o.put("code", rs.getString("code"));
                    o.put("title", rs.getString("title"));
                    o.put("credits", rs.getInt("credits"));
                    o.put("department", rs.getString("department"));
                    o.put("professor", rs.getString("professor") == null ? "TBD" : rs.getString("professor"));
                    o.put("grade", rs.getString("grade") == null ? JSONObject.NULL : rs.getString("grade"));
                    arr.put(o);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return arr;
    }

    private JSONArray handleDropList(int studentId) {
        JSONArray arr = new JSONArray();
        if (studentId < 0) return arr;
        String sql =
            "SELECT c.course_id, c.code, c.title, c.credits, d.name AS department, " +
            "u.full_name AS professor " +
            "FROM registrations r " +
            "JOIN courses c ON r.course_id = c.course_id " +
            "JOIN departments d ON c.dept_id = d.dept_id " +
            "LEFT JOIN users u ON c.professor_id = u.user_id " +
            "WHERE r.student_id = ? AND LOWER(r.status) = 'registered' AND r.grade IS NULL";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject o = new JSONObject();
                    o.put("course_id", rs.getInt("course_id"));
                    o.put("code", rs.getString("code"));
                    o.put("title", rs.getString("title"));
                    o.put("credits", rs.getInt("credits"));
                    o.put("department", rs.getString("department"));
                    o.put("professor", rs.getString("professor") == null ? "TBD" : rs.getString("professor"));
                    arr.put(o);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return arr;
    }

    private JSONArray handleCatalog(HttpServletRequest request) {
        JSONArray arr = new JSONArray();
        String department = defaultIfNull(request.getParameter("department"));
        String credits = defaultIfNull(request.getParameter("credits"));
        String professor = defaultIfNull(request.getParameter("professor"));

        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT c.code, c.title, c.credits, c.semester_num, d.name AS department, u.full_name AS professor " +
            "FROM courses c " +
            "JOIN departments d ON c.dept_id = d.dept_id " +
            "LEFT JOIN users u ON c.professor_id = u.user_id " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (!department.isEmpty()) {
            sqlBuilder.append(" AND c.dept_id = ?::int");
            params.add(department);
        }
        if (!credits.isEmpty()) {
            sqlBuilder.append(" AND c.credits = ?::int");
            params.add(credits);
        }
        if (!professor.isEmpty()) {
            sqlBuilder.append(" AND u.full_name ILIKE ?");
            params.add("%" + professor + "%");
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject o = new JSONObject();
                    o.put("code", rs.getString("code"));
                    o.put("title", rs.getString("title"));
                    o.put("credits", rs.getInt("credits"));
                    o.put("department", rs.getString("department"));
                    o.put("professor", rs.getString("professor") == null ? "TBD" : rs.getString("professor"));
                    o.put("semester_num", rs.getInt("semester_num"));
                    arr.put(o);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return arr;
    }

    

    private void handleRegisterByCode(HttpServletRequest request, HttpServletResponse response, int studentId)
            throws IOException {
        JSONObject result = new JSONObject();
        String code = request.getParameter("code");
        if (code == null || code.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("error", "Course code is required");
            response.getWriter().print(result.toString());
            return;
        }
        code = code.trim();

        String courseInfoQuery = "SELECT course_id, semester_num FROM courses WHERE code = ?";
        String studentSemesterQuery = "SELECT semester FROM users WHERE user_id = ? AND role='student'";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            int courseId = -1;
            int courseSemester = -1;
            int studentCurrentSemester = -1;

            
            try (PreparedStatement findCourseStmt = conn.prepareStatement(courseInfoQuery)) {
                findCourseStmt.setString(1, code);
                try (ResultSet rs = findCourseStmt.executeQuery()) {
                    if (!rs.next()) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        result.put("error", "Course with code '" + escapeJson(code) + "' not found.");
                        response.getWriter().print(result.toString());
                        return;
                    }
                    courseId = rs.getInt("course_id");
                    courseSemester = rs.getInt("semester_num");
                }
            }

            
            try (PreparedStatement findStudentSemesterStmt = conn.prepareStatement(studentSemesterQuery)) {
                findStudentSemesterStmt.setInt(1, studentId);
                try (ResultSet rs = findStudentSemesterStmt.executeQuery()) {
                    if (!rs.next()) {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        result.put("error", "Student information not found. Please log in again.");
                        response.getWriter().print(result.toString());
                        return;
                    }
                    studentCurrentSemester = rs.getInt("semester");
                }
            }

           
            if (courseSemester > studentCurrentSemester) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("error", "You cannot register for this course. It is for semester " + courseSemester + " and you are currently in semester " + studentCurrentSemester + ".");
                response.getWriter().print(result.toString());
                return;
            }

            
            String checkExistingSql = "SELECT 1 FROM registrations WHERE student_id = ? AND course_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkExistingSql)) {
                checkStmt.setInt(1, studentId);
                checkStmt.setInt(2, courseId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        result.put("error", "You are already registered for this course.");
                        response.getWriter().print(result.toString());
                        return;
                    }
                }
            }

            String countRegisteredSql = "SELECT COUNT(*) FROM registrations WHERE course_id = ? AND status = 'registered'";
            int registeredCount = 0;
            try (PreparedStatement countStmt = conn.prepareStatement(countRegisteredSql)) {
                countStmt.setInt(1, courseId);
                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        registeredCount = rs.getInt(1);
                    }
                }
            }

            String insertSql = "INSERT INTO registrations (student_id, course_id, status) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, studentId);
                insertStmt.setInt(2, courseId);
                if (registeredCount < 5) {
                    insertStmt.setString(3, "registered");
                    insertStmt.executeUpdate();
                    result.put("message", "Registered successfully for course " + escapeJson(code) + ".");
                } else {
                    insertStmt.setString(3, "waitlisted");
                    insertStmt.executeUpdate();
                    result.put("message", "Course is full. You have been placed on the waitlist for " + escapeJson(code) + ".");
                }
                response.getWriter().print(result.toString());
            }
        } catch (SQLException e) {
            String errorMessage = "Database error: " + escapeJson(e.getMessage());
            if (e.getMessage().contains("duplicate key value violates unique constraint")) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                errorMessage = "You are already registered for this course or a similar conflict occurred.";
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            result.put("error", errorMessage);
            response.getWriter().print(result.toString());
        }
    }

    private void handleDropCourse(HttpServletRequest request, HttpServletResponse response, int studentId)
        throws IOException {
        JSONObject result = new JSONObject();
        String courseIdStr = request.getParameter("course_id");
        if (courseIdStr == null || courseIdStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("error", "Course ID is required");
            response.getWriter().print(result.toString());
            return;
        }

        int courseId;
        try {
            courseId = Integer.parseInt(courseIdStr.trim());
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("error", "Invalid Course ID format. Must be a number.");
            response.getWriter().print(result.toString());
            return;
        }

        String sql = "DELETE FROM registrations WHERE student_id = ? AND course_id = ? AND grade IS NULL";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                result.put("message", "Course dropped successfully.");

                
                String promoteSql = "UPDATE registrations SET status = 'registered' " +
                                    "WHERE reg_id = (" +
                                    "  SELECT reg_id FROM registrations " +
                                    "  WHERE course_id = ? AND status = 'waitlisted' " +
                                    "  ORDER BY registration_date ASC LIMIT 1" +
                                    ")";
                try (PreparedStatement promoteStmt = conn.prepareStatement(promoteSql)) {
                    promoteStmt.setInt(1, courseId);
                    promoteStmt.executeUpdate();
                }
            }
            response.getWriter().print(result.toString());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("error", "Database error: " + escapeJson(e.getMessage()));
            response.getWriter().print(result.toString());
        }
    }

    private String defaultIfNull(String s) {
        return s == null ? "" : s;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        String escaped = text.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        escaped = escaped.replace("\b", "\\b");
        escaped = escaped.replace("\f", "\\f");
        escaped = escaped.replace("\n", "\\n");
        escaped = escaped.replace("\r", "\\r");
        escaped = escaped.replace("\t", "\\t");
        return escaped;
    }
}