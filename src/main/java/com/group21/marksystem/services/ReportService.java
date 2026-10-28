package com.group21.marksystem.services;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.utils.CSVHelper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Service class for generating reports and statistics.
 *
 * @author Group 21
 */
public class ReportService {
    private MarkService markService;
    private AuthenticationService userService;
    private ReadWriteLock lock;
    private List<ActivityLogEntry> activityLog;

    public ReportService(MarkService markService, AuthenticationService userService) {
        this.markService = markService;
        this.userService = userService;
        this.lock = new ReentrantReadWriteLock();
        this.activityLog = new ArrayList<>();
    }

    // ========== Grade Reports ==========

    /**
     * Generates a report for a single student showing all their marks and GPA.
     *
     * @param studentId The student ID.
     * @return A formatted report string.
     */
    public String generateStudentReport(String studentId) {
        lock.readLock().lock();
        try {
            Student student = markService.getStudent(studentId);
            if (student == null) {
                return "Error: Student not found.";
            }

            StringBuilder report = new StringBuilder();
            report.append("\n═══════════════════════════════════════════════════════════\n");
            report.append("  STUDENT MARK REPORT\n");
            report.append("═══════════════════════════════════════════════════════════\n\n");

            report.append(String.format("Student ID: %s\n", student.getStudentId()));
            report.append(String.format("Full Name: %s\n", student.getFullName()));
            report.append(String.format("Program: %s\n", student.getProgram()));
            report.append(String.format("Year: %d\n", student.getYear()));
            report.append(String.format("Email: %s\n\n", student.getEmail()));

            report.append("Marks by Subject:\n");
            report.append("───────────────────────────────────────────────────────────\n");
            report.append(String.format("%-12s %-35s %8s %s\n",
                    "Subject Code", "Subject Name", "Score %", "Grade"));
            report.append("───────────────────────────────────────────────────────────\n");

            double totalCredits = 0;
            double totalPoints = 0;

            for (Map.Entry<String, Mark> entry : student.getMarks().entrySet()) {
                Subject subject = markService.getSubject(entry.getKey());
                Mark mark = entry.getValue();

                if (subject != null && mark != null) {
                    report.append(String.format("%-12s %-35s %8.2f %s%s\n",
                            subject.getSubjectCode(),
                            subject.getSubjectName(),
                            mark.getWeightedTotal(),
                            mark.getLetterGrade(),
                            mark.isMissing() ? " (INCOMPLETE)" : ""));

                    if (mark.getLetterGrade() != null && !mark.isMissing()) {
                        totalPoints += convertGradeToPoints(mark.getLetterGrade()) * subject.getCredits();
                        totalCredits += subject.getCredits();
                    }
                }
            }

            report.append("───────────────────────────────────────────────────────────\n\n");

            double gpa = totalCredits > 0 ? totalPoints / totalCredits : 0.0;
            report.append(String.format("Cumulative GPA: %.2f / 4.00\n", gpa));

            // Check for missing marks
            List<String> missingSubjects = student.getMarks().entrySet().stream()
                    .filter(e -> e.getValue() == null || e.getValue().isMissing())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (!missingSubjects.isEmpty()) {
                report.append("\n⚠  MISSING MARKS for subjects: ");
                report.append(String.join(", ", missingSubjects));
                report.append("\n");
            }

            return report.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    private double convertGradeToPoints(Mark.Grade grade) {
        switch (grade) {
            case A_PLUS: return 4.0;
            case A:  return 4.0;
            case B_PLUS: return 3.5;
            case B:  return 3.0;
            case C_PLUS: return 2.5;
            case C:  return 2.0;
            case D:  return 1.0;
            case F:  return 0.0;
            default: return 0.0;
        }
    }

    // ========== Subject Reports ==========

    /**
     * Generates a report for a subject showing all student marks.
     *
     * @param subjectCode The subject code.
     * @return A formatted report string.
     */
    public String generateSubjectReport(String subjectCode) {
        lock.readLock().lock();
        try {
            Subject subject = markService.getSubject(subjectCode);
            if (subject == null) {
                return "Error: Subject not found.";
            }

            StringBuilder report = new StringBuilder();
            report.append("\n═══════════════════════════════════════════════════════════\n");
            report.append("  SUBJECT MARK REPORT\n");
            report.append("═══════════════════════════════════════════════════════════\n\n");

            report.append(String.format("Subject Code: %s\n", subject.getSubjectCode()));
            report.append(String.format("Subject Name: %s\n", subject.getSubjectName()));
            report.append(String.format("Credits: %d\n", subject.getCredits()));
            report.append(String.format("Description: %s\n\n", subject.getDescription()));

            // Assessment breakdown
            report.append("Assessment Categories:\n");
            for (Map.Entry<String, Double> entry : subject.getAssessmentWeights().entrySet()) {
                report.append(String.format("  %-20s %.1f%%\n", entry.getKey(), entry.getValue()));
            }
            report.append("\n");

            // Student marks table
            report.append("Student Marks:\n");
            report.append("───────────────────────────────────────────────────────────\n");
            report.append(String.format("%-10s %-25s %8s %s\n",
                    "Student ID", "Student Name", "Score %", "Grade"));
            report.append("───────────────────────────────────────────────────────────\n");

            List<Student> allStudents = markService.getAllStudents();

            for (Student student : allStudents) {
                Mark mark = student.getMark(subjectCode);
                if (mark != null) {
                    report.append(String.format("%-10s %-25s %8.2f %s%s\n",
                            student.getStudentId(),
                            student.getFullName(),
                            mark.getWeightedTotal(),
                            mark.getLetterGrade(),
                            mark.isMissing() ? " (INCOMPLETE)" : ""));
                }
            }

            report.append("───────────────────────────────────────────────────────────\n\n");

            // Statistical summary
            report.append("Statistical Summary:\n");
            long entered = allStudents.stream()
                    .map(s -> s.getMark(subjectCode))
                    .filter(Objects::nonNull)
                    .count();
            report.append(String.format("  Students with marks: %d of %d%n",
                    entered, allStudents.size()));

            DoubleSummaryStatistics stats = allStudents.stream()
                    .map(s -> s.getMark(subjectCode))
                    .filter(Objects::nonNull)
                    .filter(m -> !m.isMissing())
                    .mapToDouble(Mark::getWeightedTotal)
                    .summaryStatistics();

            if (stats.getCount() > 0) {
                report.append(String.format("  Class Average: %.2f%n", stats.getAverage()));
                report.append(String.format("  Highest Mark: %.2f%n", stats.getMax()));
                report.append(String.format("  Lowest Mark: %.2f%n", stats.getMin()));
            }

            // Students with no mark at all are missing too, matching the
            // dedicated missing-marks report.
            int missingCount = markService.getStudentsWithMissingMarks(subjectCode).size();
            if (missingCount > 0) {
                report.append(String.format("%n  ⚠  Students with missing or absent marks: %d%n",
                        missingCount));
            }

            return report.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ========== Missing Marks Report ==========

    /**
     * Generates a comprehensive report of all students with missing marks
     * across all subjects.
     *
     * @return A formatted report string.
     */
    public String generateMissingMarksReport() {
        lock.readLock().lock();
        try {
            StringBuilder report = new StringBuilder();
            report.append("\n═══════════════════════════════════════════════════════════\n");
            report.append("  MISSING MARKS REPORT\n");
            report.append("═══════════════════════════════════════════════════════════\n\n");

            List<Subject> subjects = markService.getAllSubjects();
            List<Student> students = markService.getAllStudents();

            boolean foundAny = false;

            for (Subject subject : subjects) {
                List<Map.Entry<Student, Mark>> missing = markService
                        .getStudentsWithMissingMarks(subject.getSubjectCode());

                if (!missing.isEmpty()) {
                    foundAny = true;
                    report.append(String.format("Subject: %s - %s\n",
                            subject.getSubjectCode(), subject.getSubjectName()));
                    report.append("───────────────────────────────────────────────────────────\n");

                    for (Map.Entry<Student, Mark> entry : missing) {
                        report.append(String.format("  %-10s %-25s %s\n",
                                entry.getKey().getStudentId(),
                                entry.getKey().getFullName(),
                                entry.getValue() == null ? "NO MARKS ENTERED" : "INCOMPLETE ENTRY"));
                    }
                    report.append("\n");
                }
            }

            if (!foundAny) {
                report.append("No missing marks found. All entries are complete.\n");
            }

            return report.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ========== CSV Export ==========

    /**
     * Exports all marks data to a CSV file.
     *
     * @param filePath The output file path.
     * @throws IOException If file cannot be written.
     */
    public void exportMarksToCSV(String filePath) throws IOException {
        lock.readLock().lock();
        try {
            CSVHelper.writeMarksCSV(filePath,
                    markService.getAllStudents(), markService.getAllSubjects());
        } finally {
            lock.readLock().unlock();
        }
        // logActivity needs the write lock, so it must run after the read lock
        // is released (a read-to-write upgrade would deadlock).
        logActivity("Exported marks to CSV: " + filePath);
    }

    // ========== Activity Logging ==========

    /**
     * Logs an activity entry.
     *
     * @param activity The activity description.
     */
    public void logActivity(String activity) {
        lock.writeLock().lock();
        try {
            activityLog.add(new ActivityLogEntry(activity));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Generates an activity log report.
     *
     * @return A formatted activity log string.
     */
    public String generateActivityLogReport() {
        lock.readLock().lock();
        try {
            StringBuilder report = new StringBuilder();
            report.append("\n═══════════════════════════════════════════════════════════\n");
            report.append("  ACTIVITY LOG\n");
            report.append("═══════════════════════════════════════════════════════════\n\n");

            for (ActivityLogEntry entry : activityLog) {
                report.append(entry.toString()).append("\n");
            }

            if (activityLog.isEmpty()) {
                report.append("No activity logged.\n");
            }

            return report.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Inner class for activity log entries.
     */
    private static class ActivityLogEntry {
        private LocalDateTime timestamp;
        private String activity;

        public ActivityLogEntry(String activity) {
            this.timestamp = LocalDateTime.now();
            this.activity = activity;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return String.format("[%s] %s", timestamp.format(formatter), activity);
        }
    }
}
