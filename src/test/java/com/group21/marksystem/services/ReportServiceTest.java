package com.group21.marksystem.services;

import com.group21.marksystem.db.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the report output shape after the weighting changes.
 */
class ReportServiceTest {

    @TempDir
    Path tempDir;

    private MarkService markService;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        Database database = new Database(tempDir.resolve("marks.db").toString());
        markService = new MarkService(database);
        markService.seedIfEmpty();
        UserService userService = new UserService(database);
        userService.initializeDefaultUsers();
        reportService = new ReportService(markService, userService);
    }

    @Test
    void studentReportShowsASingleWeightedScoreColumn() {
        String studentId = markService.getAllStudents().get(0).getStudentId();

        String report = reportService.generateStudentReport(studentId);

        assertTrue(report.contains("Score %"));
        assertFalse(report.contains("Weighted"));
        assertTrue(report.contains("Cumulative GPA"));
    }

    @Test
    void subjectReportCountsMissingStudentsConsistently() {
        String report = reportService.generateSubjectReport("CIV260S");

        assertTrue(report.contains("Students with marks:"));
        assertTrue(report.contains("Statistical Summary"));
        assertTrue(report.contains("Students with missing or absent marks:"));
    }

    @Test
    void exportsMarksToCsv() throws IOException {
        Path export = tempDir.resolve("report-export.csv");

        reportService.exportMarksToCSV(export.toString());

        assertTrue(Files.exists(export));
        assertTrue(Files.size(export) > 0);
    }
}
