package com.group21.marksystem.utils;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for reading, writing and generating CSV files.
 *
 * <p>Marks files use one row per student per module:</p>
 * <pre>
 * StudentID,FirstName,LastName,Email,Program,Year,SubjectCode,SubjectName,
 * Assessment1,Assessment2,Assessment3,Assessment4,Assessment5
 * </pre>
 *
 * @author Group 21
 */
public class CSVHelper {

    /** Columns that identify the student and module. */
    public static final String[] BASE_HEADERS = {
            "StudentID", "FirstName", "LastName", "Email", "Program", "Year",
            "SubjectCode", "SubjectName"
    };

    /** Minimum number of assessment columns written to a template. */
    public static final int MIN_ASSESSMENT_COLUMNS = 5;

    /**
     * Reads a CSV file and returns the data as a list of string arrays.
     * Completely blank rows are ignored.
     *
     * @param filePath Path to the CSV file.
     * @return List of rows, each row being an array of string values.
     */
    public static List<String[]> readCSV(String filePath) throws IOException {
        List<String[]> data = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (isBlankRow(line)) {
                    continue;
                }
                data.add(line);
            }
        } catch (CsvValidationException e) {
            throw new IOException("Error parsing CSV file: " + e.getMessage(), e);
        }

        return data;
    }

    /**
     * @param row A CSV row.
     * @return true when the row is null, empty, or contains only blank cells.
     */
    public static boolean isBlankRow(String[] row) {
        if (row == null || row.length == 0) {
            return true;
        }
        for (String value : row) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Writes data to a CSV file.
     *
     * @param filePath Path to output the CSV file.
     * @param headers  Header row.
     * @param data     Data rows.
     */
    public static void writeCSV(String filePath, String[] headers, List<String[]> data) throws IOException {
        Path path = Paths.get(filePath).toAbsolutePath();
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
             CSVWriter csvWriter = new CSVWriter(writer,
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.DEFAULT_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END)) {

            if (headers != null) {
                csvWriter.writeNext(headers);
            }

            for (String[] row : data) {
                csvWriter.writeNext(row);
            }
        }
    }

    /**
     * Recursively processes all CSV files in a directory.
     *
     * @param directoryPath Path to the directory.
     * @return List of all CSV file paths found.
     */
    public static List<String> findCSVFiles(String directoryPath) {
        List<String> csvFiles = new ArrayList<>();
        File dir = new File(directoryPath);

        if (dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv"));
            if (files != null) {
                for (File file : files) {
                    csvFiles.add(file.getAbsolutePath());
                }
            }

            File[] subDirs = dir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    csvFiles.addAll(findCSVFiles(subDir.getAbsolutePath()));
                }
            }
        }

        return csvFiles;
    }

    /**
     * Builds the header row for a marks template or export.
     *
     * @param assessmentColumns Number of assessment columns.
     * @return Header names in template order.
     */
    public static String[] markHeaders(int assessmentColumns, boolean includeComputed) {
        List<String> headers = new ArrayList<>(List.of(BASE_HEADERS));
        for (int i = 1; i <= assessmentColumns; i++) {
            headers.add("Assessment" + i);
        }
        if (includeComputed) {
            headers.add("WeightedTotal");
            headers.add("Grade");
        }
        return headers.toArray(new String[0]);
    }

    /**
     * @param subjects The subjects in the system.
     * @return The number of assessment columns needed to represent every module.
     */
    public static int assessmentColumnCount(Collection<Subject> subjects) {
        int max = MIN_ASSESSMENT_COLUMNS;
        for (Subject subject : subjects) {
            max = Math.max(max, subject.getAssessmentWeights().size());
        }
        return max;
    }

    /**
     * Generates a lecturer-ready marks template. Every seeded student appears
     * once with their details and a module suggested from their programme and
     * year; the lecturer only fills in the assessment marks.
     *
     * @param filePath Path to the output CSV file.
     * @param students Students to include in the roster.
     * @param subjects Modules that may be suggested.
     * @return Number of student rows written.
     */
    public static int generateTemplate(String filePath, List<Student> students,
                                       List<Subject> subjects) throws IOException {
        int assessmentColumns = assessmentColumnCount(subjects);
        String[] headers = markHeaders(assessmentColumns, false);

        List<String[]> rows = new ArrayList<>();
        for (Student student : students) {
            Subject suggested = suggestedSubject(student, subjects);
            String[] row = new String[headers.length];
            row[0] = student.getStudentId();
            row[1] = student.getFirstName();
            row[2] = student.getLastName();
            row[3] = student.getEmail();
            row[4] = student.getProgram();
            row[5] = String.valueOf(student.getYear());
            row[6] = suggested != null ? suggested.getSubjectCode() : "";
            row[7] = suggested != null ? suggested.getSubjectName() : "";
            for (int i = BASE_HEADERS.length; i < row.length; i++) {
                row[i] = "";
            }
            rows.add(row);
        }

        writeCSV(filePath, headers, rows);
        return rows.size();
    }

    /**
     * Writes all marks in the same one-row-per-student-per-module layout used
     * by the import template, so exported files can be edited and re-imported.
     *
     * @param filePath Output file path.
     * @param students Students to export.
     * @param subjects Subjects to export.
     * @return Number of rows written.
     */
    public static int writeMarksCSV(String filePath, Collection<Student> students,
                                    Collection<Subject> subjects) throws IOException {
        List<Subject> subjectList = new ArrayList<>(subjects);
        int assessmentColumns = assessmentColumnCount(subjectList);
        String[] headers = markHeaders(assessmentColumns, true);

        List<String[]> rows = new ArrayList<>();
        for (Student student : students) {
            boolean wroteAny = false;

            for (Subject subject : subjectList) {
                Mark mark = student.getMark(subject.getSubjectCode());
                if (mark == null) {
                    continue;
                }
                wroteAny = true;

                String[] row = new String[headers.length];
                row[0] = student.getStudentId();
                row[1] = student.getFirstName();
                row[2] = student.getLastName();
                row[3] = student.getEmail();
                row[4] = student.getProgram();
                row[5] = String.valueOf(student.getYear());
                row[6] = subject.getSubjectCode();
                row[7] = subject.getSubjectName();

                int index = BASE_HEADERS.length;
                for (String category : subject.getAssessmentWeights().keySet()) {
                    Double value = mark.getAssessmentMarks().get(category);
                    row[index++] = value != null ? format(value) : "";
                }
                while (index < BASE_HEADERS.length + assessmentColumns) {
                    row[index++] = "";
                }

                row[index++] = mark.isMissing() ? "" : format(mark.getWeightedTotal());
                row[index] = mark.getLetterGrade() != null ? mark.getLetterGrade().toString() : "";

                rows.add(row);
            }

            if (!wroteAny) {
                String[] row = new String[headers.length];
                row[0] = student.getStudentId();
                row[1] = student.getFirstName();
                row[2] = student.getLastName();
                row[3] = student.getEmail();
                row[4] = student.getProgram();
                row[5] = String.valueOf(student.getYear());
                for (int i = BASE_HEADERS.length; i < row.length; i++) {
                    row[i] = "";
                }
                rows.add(row);
            }
        }

        writeCSV(filePath, headers, rows);
        return rows.size();
    }

    /**
     * Picks the most relevant module for a student.
     *
     * @param student  The student.
     * @param subjects Available modules.
     * @return A matching module, or the first module, or null.
     */
    public static Subject suggestedSubject(Student student, List<Subject> subjects) {
        if (subjects.isEmpty()) {
            return null;
        }

        String programme = student.getProgram() == null ? "" : student.getProgram();
        String yearMarker = "Year " + student.getYear();

        for (Subject subject : subjects) {
            String description = subject.getDescription() == null ? "" : subject.getDescription();
            if (description.startsWith(programme) && description.contains(yearMarker)) {
                return subject;
            }
        }
        for (Subject subject : subjects) {
            String description = subject.getDescription() == null ? "" : subject.getDescription();
            if (description.startsWith(programme)) {
                return subject;
            }
        }
        return subjects.get(0);
    }

    private static String format(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }
}
