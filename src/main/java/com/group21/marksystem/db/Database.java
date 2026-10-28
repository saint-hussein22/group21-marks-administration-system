package com.group21.marksystem.db;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.models.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite persistence layer for the Marks Administration System.
 *
 * <p>Both the CLI and the GUI share this database, so a change made in one
 * interface is visible the next time data is loaded in the other. The database
 * file defaults to {@code data/marks.db} and can be overridden with the
 * {@code marksystem.db} system property.</p>
 *
 * @author Group 21
 */
public class Database implements MarkRepository {

    /** Default location of the SQLite database file. */
    public static final String DEFAULT_PATH = "data/marks.db";

    /** System property used to override the database file location. */
    public static final String PATH_PROPERTY = "marksystem.db";

    private static Database instance;

    private static final String SEED_KEY = "seeded";

    private final Path dbPath;

    /**
     * Creates a database handle and ensures the schema exists.
     *
     * @param dbPath Path to the SQLite database file.
     */
    public Database(String dbPath) {
        this.dbPath = Paths.get(dbPath).toAbsolutePath();
        initialize();
    }

    /**
     * Returns the shared database instance used by the CLI and GUI.
     *
     * @return The shared database.
     */
    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database(System.getProperty(PATH_PROPERTY, DEFAULT_PATH));
        }
        return instance;
    }

    /**
     * @return Absolute path of the database file.
     */
    public String getPath() {
        return dbPath.toString();
    }

    // ========== Schema ==========

    private void initialize() {
        try {
            Path parent = dbPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS students ("
                        + "student_id TEXT PRIMARY KEY,"
                        + "first_name TEXT NOT NULL,"
                        + "last_name TEXT NOT NULL,"
                        + "email TEXT,"
                        + "program TEXT,"
                        + "year INTEGER NOT NULL)");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS subjects ("
                        + "subject_code TEXT PRIMARY KEY,"
                        + "subject_name TEXT NOT NULL,"
                        + "credits INTEGER NOT NULL,"
                        + "description TEXT)");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS subject_assessments ("
                        + "subject_code TEXT NOT NULL,"
                        + "category TEXT NOT NULL,"
                        + "weight REAL NOT NULL,"
                        + "position INTEGER NOT NULL,"
                        + "PRIMARY KEY (subject_code, category),"
                        + "FOREIGN KEY (subject_code) REFERENCES subjects(subject_code) ON DELETE CASCADE)");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS marks ("
                        + "student_id TEXT NOT NULL,"
                        + "subject_code TEXT NOT NULL,"
                        + "category TEXT NOT NULL,"
                        + "score REAL,"
                        + "entered_by TEXT,"
                        + "entered_at TEXT,"
                        + "position INTEGER NOT NULL,"
                        + "PRIMARY KEY (student_id, subject_code, category),"
                        + "FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,"
                        + "FOREIGN KEY (subject_code) REFERENCES subjects(subject_code) ON DELETE CASCADE)");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                        + "username TEXT PRIMARY KEY,"
                        + "password TEXT NOT NULL,"
                        + "role TEXT NOT NULL,"
                        + "full_name TEXT,"
                        + "department TEXT)");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS settings ("
                        + "setting_key TEXT PRIMARY KEY,"
                        + "setting_value TEXT NOT NULL)");
            }
        } catch (DatabaseException e) {
            throw e;
        } catch (IOException e) {
            throw new DatabaseException("Could not create the database directory for " + dbPath, e);
        } catch (SQLException e) {
            throw new DatabaseException("Could not initialise the database at " + dbPath, e);
        }
    }

    private Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("SQLite JDBC driver is not on the classpath.", e);
        }

        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            // Wait instead of failing immediately when another connection is writing.
            stmt.execute("PRAGMA busy_timeout = 5000");
        }
        return conn;
    }

    // ========== Students ==========

    /**
     * Inserts a student or updates the existing row with the same ID.
     *
     * @param student The student to persist.
     */
    public void saveStudent(Student student) {
        String sql = "INSERT INTO students (student_id, first_name, last_name, email, program, year) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(student_id) DO UPDATE SET "
                + "first_name = excluded.first_name, last_name = excluded.last_name, "
                + "email = excluded.email, program = excluded.program, year = excluded.year";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getStudentId());
            ps.setString(2, student.getFirstName());
            ps.setString(3, student.getLastName());
            ps.setString(4, student.getEmail());
            ps.setString(5, student.getProgram());
            ps.setInt(6, student.getYear());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save student " + student.getStudentId(), e);
        }
    }

    /**
     * Deletes a student and all of their marks.
     *
     * @param studentId The student ID.
     */
    public void deleteStudent(String studentId) {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement marks = conn.prepareStatement(
                        "DELETE FROM marks WHERE student_id = ?");
                     PreparedStatement student = conn.prepareStatement(
                             "DELETE FROM students WHERE student_id = ?")) {
                    marks.setString(1, studentId);
                    marks.executeUpdate();
                    student.setString(1, studentId);
                    student.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete student " + studentId, e);
        }
    }

    /**
     * Loads all students from the database.
     *
     * @return Map of student ID to student.
     */
    public Map<String, Student> loadStudents() {
        Map<String, Student> students = new LinkedHashMap<>();
        String sql = "SELECT student_id, first_name, last_name, email, program, year "
                + "FROM students ORDER BY student_id";

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Student student = new Student(
                        rs.getString("student_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("program"),
                        rs.getInt("year"));
                students.put(student.getStudentId(), student);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load students", e);
        }
        return students;
    }

    // ========== Subjects ==========

    /**
     * Inserts or updates a subject together with its assessment weights.
     * Marks for categories that no longer exist are removed.
     *
     * @param subject The subject to persist.
     */
    public void saveSubject(Subject subject) {
        String upsert = "INSERT INTO subjects (subject_code, subject_name, credits, description) "
                + "VALUES (?, ?, ?, ?) "
                + "ON CONFLICT(subject_code) DO UPDATE SET "
                + "subject_name = excluded.subject_name, credits = excluded.credits, "
                + "description = excluded.description";

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                    ps.setString(1, subject.getSubjectCode());
                    ps.setString(2, subject.getSubjectName());
                    ps.setInt(3, subject.getCredits());
                    ps.setString(4, subject.getDescription());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM subject_assessments WHERE subject_code = ?")) {
                    ps.setString(1, subject.getSubjectCode());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO subject_assessments (subject_code, category, weight, position) "
                                + "VALUES (?, ?, ?, ?)")) {
                    int position = 0;
                    for (Map.Entry<String, Double> entry : subject.getAssessmentWeights().entrySet()) {
                        ps.setString(1, subject.getSubjectCode());
                        ps.setString(2, entry.getKey());
                        ps.setDouble(3, entry.getValue());
                        ps.setInt(4, position++);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // Drop marks that refer to assessment categories that were removed.
                Collection<String> categories = subject.getAssessmentWeights().keySet();
                if (categories.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM marks WHERE subject_code = ?")) {
                        ps.setString(1, subject.getSubjectCode());
                        ps.executeUpdate();
                    }
                } else {
                    StringBuilder sql = new StringBuilder(
                            "DELETE FROM marks WHERE subject_code = ? AND category NOT IN (");
                    for (int i = 0; i < categories.size(); i++) {
                        sql.append(i == 0 ? "?" : ", ?");
                    }
                    sql.append(')');
                    try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                        ps.setString(1, subject.getSubjectCode());
                        int index = 2;
                        for (String category : categories) {
                            ps.setString(index++, category);
                        }
                        ps.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save subject " + subject.getSubjectCode(), e);
        }
    }

    /**
     * Deletes a subject, its assessments and all marks for it.
     *
     * @param subjectCode The subject code.
     */
    public void deleteSubject(String subjectCode) {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement marks = conn.prepareStatement(
                        "DELETE FROM marks WHERE subject_code = ?");
                     PreparedStatement assessments = conn.prepareStatement(
                             "DELETE FROM subject_assessments WHERE subject_code = ?");
                     PreparedStatement subject = conn.prepareStatement(
                             "DELETE FROM subjects WHERE subject_code = ?")) {
                    marks.setString(1, subjectCode);
                    marks.executeUpdate();
                    assessments.setString(1, subjectCode);
                    assessments.executeUpdate();
                    subject.setString(1, subjectCode);
                    subject.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete subject " + subjectCode, e);
        }
    }

    /**
     * Loads all subjects, including their assessment weights, in display order.
     *
     * @return Map of subject code to subject.
     */
    public Map<String, Subject> loadSubjects() {
        Map<String, Subject> subjects = new LinkedHashMap<>();

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT subject_code, subject_name, credits, description FROM subjects");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Subject subject = new Subject(
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        rs.getInt("credits"),
                        rs.getString("description"));
                subjects.put(subject.getSubjectCode(), subject);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load subjects", e);
        }

        String sql = "SELECT subject_code, category, weight FROM subject_assessments "
                + "ORDER BY subject_code, position";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Subject subject = subjects.get(rs.getString("subject_code"));
                if (subject != null) {
                    subject.addAssessment(rs.getString("category"), rs.getDouble("weight"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load subject assessments", e);
        }

        return subjects;
    }

    // ========== Marks ==========

    /**
     * Inserts or replaces every assessment mark for one student in one subject.
     *
     * @param mark The mark to persist.
     */
    public void saveMark(Mark mark) {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement delete = conn.prepareStatement(
                        "DELETE FROM marks WHERE student_id = ? AND subject_code = ?")) {
                    delete.setString(1, mark.getStudentId());
                    delete.setString(2, mark.getSubjectCode());
                    delete.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO marks (student_id, subject_code, category, score, entered_by, entered_at, position) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    int position = 0;
                    for (Map.Entry<String, Double> entry : mark.getAssessmentMarks().entrySet()) {
                        ps.setString(1, mark.getStudentId());
                        ps.setString(2, mark.getSubjectCode());
                        ps.setString(3, entry.getKey());
                        if (entry.getValue() == null) {
                            ps.setNull(4, java.sql.Types.REAL);
                        } else {
                            ps.setDouble(4, entry.getValue());
                        }
                        ps.setString(5, mark.getEnteredBy());
                        ps.setString(6, mark.getEnteredAt() != null
                                ? mark.getEnteredAt().toString() : LocalDateTime.now().toString());
                        ps.setInt(7, position++);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save marks for " + mark.getStudentId(), e);
        }
    }

    /**
     * Loads all marks into the supplied student objects.
     *
     * @param students The already loaded students.
     * @param subjects The already loaded subjects.
     */
    public void loadMarks(Map<String, Student> students, Map<String, Subject> subjects) {
        String sql = "SELECT student_id, subject_code, category, score, entered_by, entered_at "
                + "FROM marks ORDER BY student_id, subject_code, position";

        Map<String, Map<String, Map<String, Double>>> raw = new LinkedHashMap<>();
        Map<String, String> enteredBy = new HashMap<>();
        Map<String, LocalDateTime> enteredAt = new HashMap<>();

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String subjectCode = rs.getString("subject_code");
                String key = studentId + "|" + subjectCode;

                Double score = rs.getObject("score") == null ? null : rs.getDouble("score");
                raw.computeIfAbsent(key, k -> new LinkedHashMap<>())
                        .computeIfAbsent(subjectCode, k -> new LinkedHashMap<>())
                        .put(rs.getString("category"), score);

                String by = rs.getString("entered_by");
                if (by != null && !by.isEmpty()) {
                    enteredBy.putIfAbsent(key, by);
                }
                String at = rs.getString("entered_at");
                if (at != null && !at.isEmpty()) {
                    try {
                        enteredAt.putIfAbsent(key, LocalDateTime.parse(at));
                    } catch (RuntimeException ignored) {
                        // Ignore malformed timestamps from older data.
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load marks", e);
        }

        for (Map.Entry<String, Map<String, Map<String, Double>>> entry : raw.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            String studentId = parts[0];
            String subjectCode = parts[1];

            Student student = students.get(studentId);
            Subject subject = subjects.get(subjectCode);
            if (student == null || subject == null) {
                continue;
            }

            // Only keep the subject's current categories; rows left behind by a
            // renamed or removed assessment must not influence the total.
            Map<String, Double> stored = entry.getValue().get(subjectCode);
            Map<String, Double> scores = new LinkedHashMap<>();
            for (String category : subject.getAssessmentWeights().keySet()) {
                scores.put(category, stored.get(category));
            }

            Mark mark = new Mark(studentId, subjectCode, scores);
            mark.alignToCategories(subject.getAssessmentWeights());
            mark.setEnteredBy(enteredBy.get(entry.getKey()));
            LocalDateTime timestamp = enteredAt.get(entry.getKey());
            if (timestamp != null) {
                mark.setEnteredAt(timestamp);
            }
            student.addMark(subjectCode, mark);
        }
    }

    // ========== Users ==========

    /**
     * Inserts or updates a user.
     *
     * @param user The user to persist.
     */
    public void saveUser(User user) {
        String sql = "INSERT INTO users (username, password, role, full_name, department) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(username) DO UPDATE SET "
                + "password = excluded.password, role = excluded.role, "
                + "full_name = excluded.full_name, department = excluded.department";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getDepartment());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save user " + user.getUsername(), e);
        }
    }

    /**
     * Deletes a user.
     *
     * @param username The username.
     */
    public void deleteUser(String username) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete user " + username, e);
        }
    }

    /**
     * Loads all users.
     *
     * @return Map of username to user.
     */
    public Map<String, User> loadUsers() {
        Map<String, User> users = new LinkedHashMap<>();
        String sql = "SELECT username, password, role, full_name, department FROM users";

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User.Role role;
                try {
                    String roleText = rs.getString("role");
                    role = roleText == null ? User.Role.LECTURER : User.Role.valueOf(roleText);
                } catch (RuntimeException e) {
                    // Unknown role values fall back to the least privileged role.
                    role = User.Role.LECTURER;
                }
                User user = new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        role,
                        rs.getString("full_name"),
                        rs.getString("department"));
                users.put(user.getUsername(), user);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load users", e);
        }
        return users;
    }

    // ========== Seed state ==========

    /**
     * @return true once the demo data has been seeded into this database.
     */
    public boolean isSeeded() {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, SEED_KEY);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && Boolean.parseBoolean(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the seed state", e);
        }
    }

    /**
     * Records that the demo data has been seeded so an empty marks table is not
     * mistaken for a fresh database on the next start.
     */
    public void markSeeded() {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, SEED_KEY);
            ps.setString(2, "true");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the seed state", e);
        }
    }

    // ========== Helpers ==========

    /**
     * @return true when there are no rows in the given table.
     */
    private boolean isTableEmpty(String table) {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return !rs.next() || rs.getInt(1) == 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not query " + table, e);
        }
    }

    public boolean isStudentTableEmpty() {
        return isTableEmpty("students");
    }

    public boolean isSubjectTableEmpty() {
        return isTableEmpty("subjects");
    }

    public boolean isMarkTableEmpty() {
        return isTableEmpty("marks");
    }

    public boolean isUserTableEmpty() {
        return isTableEmpty("users");
    }

    /**
     * Runtime exception thrown when a database operation fails.
     */
    public static class DatabaseException extends RuntimeException {
        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }

        public DatabaseException(String message) {
            super(message);
        }
    }
}
