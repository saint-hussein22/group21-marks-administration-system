package com.group21.marksystem.services;

import com.group21.marksystem.db.Database;
import com.group21.marksystem.models.Mark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for subject/mark consistency and service-level validation.
 */
class MarkServiceTest {

    @TempDir
    Path tempDir;

    private Database database;
    private MarkService markService;
    private String studentId;

    @BeforeEach
    void setUp() {
        database = new Database(tempDir.resolve("marks.db").toString());
        markService = new MarkService(database);
        markService.seedIfEmpty();
        studentId = markService.getAllStudents().get(0).getStudentId();
    }

    @Test
    void renamingAnAssessmentRealignsMarksAndDatabase() throws Exception {
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("Test 1", 80.0);
        scores.put("Test 2", 70.0);
        scores.put("Exam", 90.0);
        assertTrue(markService.enterMarks(studentId, "CIV260S",
                new Mark(studentId, "CIV260S", scores), "lecturer1"));

        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("Test 1", 25.0);
        weights.put("Test 2", 25.0);
        weights.put("Final Exam", 50.0);
        assertTrue(markService.updateSubject("CIV260S", "Structural Analysis 2", 12,
                "Civil Engineering - Year 2", weights));

        Mark updated = markService.getMarks(studentId, "CIV260S");
        assertEquals(List.of("Test 1", "Test 2", "Final Exam"),
                new ArrayList<>(updated.getAssessmentMarks().keySet()));
        assertEquals(37.5, updated.getWeightedTotal(), 0.001);
        assertTrue(updated.isMissing(), "the new 50% assessment must be flagged as missing");

        Mark reloaded = new MarkService(database).getMarks(studentId, "CIV260S");
        assertEquals(List.of("Test 1", "Test 2", "Final Exam"),
                new ArrayList<>(reloaded.getAssessmentMarks().keySet()));
        assertEquals(37.5, reloaded.getWeightedTotal(), 0.001);
        assertTrue(reloaded.isMissing());

        assertEquals(Set.of("Test 1", "Test 2", "Final Exam"),
                storedCategories(studentId, "CIV260S"),
                "removed categories must not survive in the database");
    }

    @Test
    void rejectsScoresOutsideZeroToOneHundred() {
        assertThrows(IllegalArgumentException.class,
                () -> enterMark(150.0));
        assertThrows(IllegalArgumentException.class,
                () -> enterMark(-1.0));
        assertThrows(IllegalArgumentException.class,
                () -> enterMark(Double.NaN));
    }

    @Test
    void rejectsAssessmentWeightsThatDoNotAddUpToOneHundred() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("Test 1", 30.0);
        weights.put("Test 2", 30.0);
        weights.put("Exam", 30.0);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> markService.updateSubject("CIV260S", "Structural Analysis 2", 12,
                        "Civil", weights));
        assertTrue(error.getMessage().contains("100"));
    }

    @Test
    void doesNotReseedDemoMarksAfterTheyAreDeleted() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + database.getPath());
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM marks");
        }

        markService.seedIfEmpty();

        int totalMarks = new MarkService(database).getAllStudents().stream()
                .mapToInt(student -> student.getMarks().size())
                .sum();
        assertEquals(0, totalMarks);
    }

    private void enterMark(double score) {
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("Test 1", score);
        scores.put("Test 2", 70.0);
        scores.put("Exam", 90.0);
        markService.enterMarks(studentId, "CIV260S",
                new Mark(studentId, "CIV260S", scores), "lecturer1");
    }

    private Set<String> storedCategories(String studentId, String subjectCode) throws Exception {
        Set<String> categories = new LinkedHashSet<>();
        String sql = "SELECT category FROM marks WHERE student_id = ? AND subject_code = ? "
                + "ORDER BY position";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + database.getPath());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, subjectCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.add(rs.getString(1));
                }
            }
        }
        return categories;
    }
}
