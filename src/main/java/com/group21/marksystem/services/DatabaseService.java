package com.group21.marksystem.services;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.models.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Collection;
import java.util.Map;

/**
 * Embedded SQLite persistence service.
 */
public class DatabaseService {
    private final String dbUrl;

    public DatabaseService(String dbFilePath) {
        this.dbUrl = "jdbc:sqlite:" + dbFilePath;
    }

    public void initialize() throws SQLException, IOException {
        Path dbPath = Paths.get(dbUrl.replace("jdbc:sqlite:", ""));
        if (dbPath.getParent() != null) {
            Files.createDirectories(dbPath.getParent());
        }

        try (Connection connection = DriverManager.getConnection(dbUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        username TEXT PRIMARY KEY,
                        password TEXT NOT NULL,
                        role TEXT NOT NULL,
                        full_name TEXT NOT NULL,
                        department TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS students (
                        student_id TEXT PRIMARY KEY,
                        first_name TEXT NOT NULL,
                        last_name TEXT NOT NULL,
                        email TEXT NOT NULL,
                        program TEXT NOT NULL,
                        year INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS subjects (
                        subject_code TEXT PRIMARY KEY,
                        subject_name TEXT NOT NULL,
                        credits INTEGER NOT NULL,
                        description TEXT NOT NULL,
                        assessment_weights BLOB NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS marks (
                        student_id TEXT NOT NULL,
                        subject_code TEXT NOT NULL,
                        mark_data BLOB NOT NULL,
                        PRIMARY KEY (student_id, subject_code),
                        FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
                        FOREIGN KEY (subject_code) REFERENCES subjects(subject_code) ON DELETE CASCADE
                    )
                    """);
        }
    }

    public void saveAll(MarkService markService, UserService userService) throws SQLException, IOException {
        try (Connection connection = DriverManager.getConnection(dbUrl)) {
            connection.setAutoCommit(false);
            try {
                clearTables(connection);
                saveUsers(connection, userService.getAllUsers());
                saveSubjects(connection, markService.getAllSubjects());
                saveStudents(connection, markService.getAllStudents());
                saveMarks(connection, markService.getAllStudents());
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                if (e instanceof SQLException sqlException) throw sqlException;
                if (e instanceof IOException ioException) throw ioException;
                throw new RuntimeException("Unexpected database save error", e);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean loadAll(MarkService markService, UserService userService)
            throws SQLException, IOException, ClassNotFoundException {
        try (Connection connection = DriverManager.getConnection(dbUrl)) {
            int userCount = count(connection, "users");
            int studentCount = count(connection, "students");
            int subjectCount = count(connection, "subjects");
            int markCount = count(connection, "marks");

            if (userCount == 0 && studentCount == 0 && subjectCount == 0 && markCount == 0) {
                return false;
            }

            userService.clearUsers();
            markService.clearAllData();

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT username, password, role, full_name, department FROM users");
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    User user = new User(
                            rs.getString("username"),
                            rs.getString("password"),
                            User.Role.valueOf(rs.getString("role")),
                            rs.getString("full_name"),
                            rs.getString("department"));
                    userService.addUserFromStorage(user);
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT subject_code, subject_name, credits, description, assessment_weights FROM subjects");
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Subject subject = new Subject(
                            rs.getString("subject_code"),
                            rs.getString("subject_name"),
                            rs.getInt("credits"),
                            rs.getString("description"));
                    subject.setAssessmentWeights(deserializeObject(rs.getBytes("assessment_weights")));
                    markService.addSubject(subject);
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT student_id, first_name, last_name, email, program, year FROM students");
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Student student = new Student(
                            rs.getString("student_id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            rs.getString("program"),
                            rs.getInt("year"));
                    markService.addStudent(student);
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT student_id, subject_code, mark_data FROM marks");
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Mark mark = deserializeObject(rs.getBytes("mark_data"));
                    markService.enterMarks(
                            rs.getString("student_id"),
                            rs.getString("subject_code"),
                            mark,
                            mark.getEnteredBy() != null ? mark.getEnteredBy() : "Database Import");
                }
            }

            return true;
        }
    }

    private void clearTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM marks");
            statement.execute("DELETE FROM students");
            statement.execute("DELETE FROM subjects");
            statement.execute("DELETE FROM users");
        }
    }

    private void saveUsers(Connection connection, Collection<User> users) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users(username, password, role, full_name, department) VALUES(?, ?, ?, ?, ?)")) {
            for (User user : users) {
                statement.setString(1, user.getUsername());
                statement.setString(2, user.getPassword());
                statement.setString(3, user.getRole().name());
                statement.setString(4, user.getFullName());
                statement.setString(5, user.getDepartment());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void saveStudents(Connection connection, Collection<Student> students) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO students(student_id, first_name, last_name, email, program, year) VALUES(?, ?, ?, ?, ?, ?)")) {
            for (Student student : students) {
                statement.setString(1, student.getStudentId());
                statement.setString(2, student.getFirstName());
                statement.setString(3, student.getLastName());
                statement.setString(4, student.getEmail());
                statement.setString(5, student.getProgram());
                statement.setInt(6, student.getYear());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void saveSubjects(Connection connection, Collection<Subject> subjects) throws SQLException, IOException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO subjects(subject_code, subject_name, credits, description, assessment_weights) VALUES(?, ?, ?, ?, ?)")) {
            for (Subject subject : subjects) {
                statement.setString(1, subject.getSubjectCode());
                statement.setString(2, subject.getSubjectName());
                statement.setInt(3, subject.getCredits());
                statement.setString(4, subject.getDescription());
                statement.setBytes(5, serializeObject(subject.getAssessmentWeights()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void saveMarks(Connection connection, Collection<Student> students) throws SQLException, IOException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO marks(student_id, subject_code, mark_data) VALUES(?, ?, ?)")) {
            for (Student student : students) {
                for (Map.Entry<String, Mark> entry : student.getMarks().entrySet()) {
                    statement.setString(1, student.getStudentId());
                    statement.setString(2, entry.getKey());
                    statement.setBytes(3, serializeObject(entry.getValue()));
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private int count(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private byte[] serializeObject(Object object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(object);
        }
        return byteArrayOutputStream.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializeObject(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (T) objectInputStream.readObject();
        }
    }
}
