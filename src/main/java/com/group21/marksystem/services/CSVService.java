package com.group21.marksystem.services;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.utils.CSVHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Service class for handling CSV import/export of marks data.
 * Uses multithreading for background processing.
 *
 * @author Group 21
 */
public class CSVService {
    private MarkService markService;
    private ReadWriteLock lock;

    public CSVService(MarkService markService) {
        this.markService = markService;
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Asynchronously imports marks from a CSV file.
     * Uses multithreading for background processing.
     *
     * @param filePath Path to the CSV file.
     * @return CompletableFuture that completes with the number of records imported.
     */
    public CompletableFuture<Integer> importMarksFromCSVAsync(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return importMarksFromCSV(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to import CSV: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Imports marks from a CSV file.
     * Expected format: StudentID, FirstName, LastName, Email, Program, Year,
     *                  SubjectCode, Assessment1, Assessment2, ..., Total, Grade
     *
     * @param filePath Path to the CSV file.
     * @return Number of records imported.
     * @throws IOException If file cannot be read.
     */
    public int importMarksFromCSV(String filePath) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            throw new IOException("File not found: " + filePath);
        }

        List<String[]> data = CSVHelper.readCSV(filePath);
        if (data.isEmpty()) {
            return 0;
        }

        String[] headers = data.get(0);
        int importedCount = 0;

        lock.writeLock().lock();
        try {
            for (int i = 1; i < data.size(); i++) {
                String[] row = data.get(i);
                if (row.length < 6) continue;

                try {
                    // Parse student info
                    String studentId = row[0];
                    String firstName = row[1];
                    String lastName = row[2];
                    String email = row[3];
                    String program = row[4];
                    int year = Integer.parseInt(row[5]);

                    // Check if student exists, create if not
                    Student student = markService.getStudent(studentId);
                    if (student == null) {
                        student = new Student(studentId, firstName, lastName, email, program, year);
                        markService.addStudent(student);
                    }

                    // Parse marks if subject code is provided
                    if (row.length > 6 && !row[6].isEmpty()) {
                        String subjectCode = row[6];
                        Subject subject = markService.getSubject(subjectCode);

                        if (subject != null) {
                            Map<String, Double> assessmentMarks = new LinkedHashMap<>();

                            List<Map.Entry<String, Double>> weightEntries =
                                    new ArrayList<>(subject.getAssessmentWeights().entrySet());

                            // Read assessment marks starting from index 7
                            int markIndex = 7;
                            for (int j = 0; j < weightEntries.size() && markIndex < row.length; j++) {
                                String category = weightEntries.get(j).getKey();
                                String markValue = row[markIndex].trim();
                                if (!markValue.isEmpty()) {
                                    try {
                                        assessmentMarks.put(category, Double.parseDouble(markValue));
                                    } catch (NumberFormatException e) {
                                        // Skip invalid mark values
                                    }
                                } else {
                                    assessmentMarks.put(category, null);
                                }
                                markIndex++;
                            }

                            Mark mark = new Mark(studentId, subjectCode, assessmentMarks);
                            markService.enterMarks(studentId, subjectCode, mark, "CSV Import");
                        }
                    }

                    importedCount++;
                } catch (NumberFormatException e) {
                    System.err.println("Skipping row " + (i + 1) + ": Invalid data format");
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        return importedCount;
    }

    /**
     * Exports all marks data to a CSV file synchronously.
     *
     * @param filePath Path to the output CSV file.
     * @return Number of records exported.
     * @throws IOException If file cannot be written.
     */
    public int exportMarksToCSV(String filePath) throws IOException {
        List<Student> students = markService.getAllStudents();
        List<Subject> subjects = markService.getAllSubjects();

        List<String> headers = new ArrayList<>();
        headers.add("StudentID");
        headers.add("FirstName");
        headers.add("LastName");
        headers.add("Email");
        headers.add("Program");
        headers.add("Year");

        for (Subject subject : subjects) {
            for (String category : subject.getAssessmentWeights().keySet()) {
                headers.add(subject.getSubjectCode() + "_" + category);
            }
            headers.add(subject.getSubjectCode() + "_WeightedTotal");
            headers.add(subject.getSubjectCode() + "_Grade");
        }

        List<String[]> rows = new ArrayList<>();

        for (Student student : students) {
            List<String> row = new ArrayList<>();
            row.add(student.getStudentId());
            row.add(student.getFirstName());
            row.add(student.getLastName());
            row.add(student.getEmail());
            row.add(student.getProgram());
            row.add(String.valueOf(student.getYear()));

            for (Subject subject : subjects) {
                Mark mark = student.getMark(subject.getSubjectCode());
                if (mark != null) {
                    for (String category : subject.getAssessmentWeights().keySet()) {
                        Double markValue = mark.getAssessmentMarks().get(category);
                        row.add(markValue != null ? String.valueOf(markValue) : "MISSING");
                    }
                    row.add(String.format("%.2f", mark.isMissing() ? 0.0 : mark.getWeightedTotal()));
                    row.add(mark.getLetterGrade().toString());
                } else {
                    for (int j = 0; j <= subject.getAssessmentWeights().size() + 1; j++) {
                        row.add("");
                    }
                }
            }

            rows.add(row.toArray(new String[0]));
        }

        CSVHelper.writeCSV(filePath, headers.toArray(new String[0]), rows);
        return rows.size();
    }

    /**
     * Asynchronously exports marks data to a CSV file.
     *
     * @param filePath Path to the output CSV file.
     * @return CompletableFuture that completes with the number of records exported.
     */
    public CompletableFuture<Integer> exportMarksToCSVAsync(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return exportMarksToCSV(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to export CSV: " + e.getMessage(), e);
            }
        });
    }
}
