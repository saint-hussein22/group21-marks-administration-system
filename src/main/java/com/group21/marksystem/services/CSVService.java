package com.group21.marksystem.services;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.utils.CSVHelper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service class for handling CSV import/export of marks data.
 * Uses multithreading for background processing.
 *
 * @author Group 21
 */
public class CSVService {

    private static final Pattern ASSESSMENT_COLUMN =
            Pattern.compile("(?i)^assessment\\s*[_]?\\s*(\\d+)$");

    private final MarkService markService;

    public CSVService(MarkService markService) {
        this.markService = markService;
    }

    /**
     * Downloads a marks template populated with the current class list.
     *
     * @param filePath Destination path.
     * @return Number of student rows written.
     * @throws IOException If the file cannot be written.
     */
    public int generateTemplate(String filePath) throws IOException {
        try {
            return markService.withReadLock(() -> {
                try {
                    return CSVHelper.generateTemplate(filePath,
                            markService.getAllStudents(), markService.getAllSubjects());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Asynchronously imports marks from a CSV file.
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
     * Asynchronously imports marks and returns the full import report.
     *
     * @param filePath Path to the CSV file.
     * @return CompletableFuture that completes with the import summary.
     */
    public CompletableFuture<ImportSummary> importWithReportAsync(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return importMarksFromCSVDetailed(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to import CSV: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Imports marks from a CSV file.
     *
     * @param filePath Path to the CSV file.
     * @return Number of marks imported.
     * @throws IOException If the file cannot be read or the header is invalid.
     */
    public int importMarksFromCSV(String filePath) throws IOException {
        return importMarksFromCSVDetailed(filePath).getMarksSaved();
    }

    /**
     * Imports marks from a CSV file and reports what changed.
     *
     * <p>The header is read by name, so column order does not matter. The
     * expected layout is the one produced by
     * {@link #generateTemplate(String)}. Existing students are updated from the
     * file and existing marks are replaced.</p>
     *
     * @param filePath Path to the CSV file.
     * @return A summary of the import.
     * @throws IOException If the file cannot be read or the header is invalid.
     */
    public ImportSummary importMarksFromCSVDetailed(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }

        List<String[]> data = CSVHelper.readCSV(filePath);
        ImportSummary summary = new ImportSummary();
        if (data.isEmpty()) {
            summary.addWarning("The CSV file is empty.");
            return summary;
        }

        Map<String, Integer> columns = headerIndexes(data.get(0));
        if (!columns.containsKey("studentid")) {
            throw new IOException("The CSV file must contain a StudentID column. "
                    + "Download the template from the CSV tab and use that layout.");
        }

        List<Integer> assessmentColumns = assessmentColumns(data.get(0));
        if (assessmentColumns.isEmpty()) {
            throw new IOException("The CSV file must contain Assessment1, Assessment2, ... columns.");
        }

        Set<String> savedMarks = new HashSet<>();
        markService.withWriteLock(() -> {
            for (int rowIndex = 1; rowIndex < data.size(); rowIndex++) {
                String[] row = data.get(rowIndex);
                int lineNumber = rowIndex + 1;
                summary.rowsProcessed++;

                String studentId = value(row, columns, "studentid");
                if (studentId.isEmpty()) {
                    summary.addWarning("Line " + lineNumber + ": missing StudentID - row skipped.");
                    continue;
                }

                String firstName = value(row, columns, "firstname");
                String lastName = value(row, columns, "lastname");
                String email = value(row, columns, "email");
                String program = value(row, columns, "program");
                String yearText = value(row, columns, "year");

                int year = 1;
                if (!yearText.isEmpty()) {
                    try {
                        year = Integer.parseInt(yearText);
                        if (year < 1 || year > 10) {
                            summary.addWarning("Line " + lineNumber + ": invalid Year '" + yearText
                                    + "' - using 1.");
                            year = 1;
                        }
                    } catch (NumberFormatException e) {
                        summary.addWarning("Line " + lineNumber + ": invalid Year '" + yearText
                                + "' - using 1.");
                    }
                }

                Student student = markService.getStudent(studentId);
                if (student == null) {
                    if (firstName.isEmpty() || lastName.isEmpty()) {
                        summary.addWarning("Line " + lineNumber + ": new student " + studentId
                                + " needs FirstName and LastName - row skipped.");
                        continue;
                    }
                    student = new Student(studentId, firstName, lastName,
                            email.isEmpty() ? studentId + "@mycput.ac.za" : email,
                            program.isEmpty() ? "Unspecified" : program, year);
                    markService.addStudent(student);
                    summary.studentsCreated++;
                } else {
                    String newFirst = firstName.isEmpty() ? student.getFirstName() : firstName;
                    String newLast = lastName.isEmpty() ? student.getLastName() : lastName;
                    String newEmail = email.isEmpty() ? student.getEmail() : email;
                    String newProgram = program.isEmpty() ? student.getProgram() : program;
                    int newYear = yearText.isEmpty() ? student.getYear() : year;

                    boolean changed = !newFirst.equals(student.getFirstName())
                            || !newLast.equals(student.getLastName())
                            || !newEmail.equals(student.getEmail())
                            || !newProgram.equals(student.getProgram())
                            || newYear != student.getYear();

                    if (changed) {
                        markService.updateStudent(studentId, newFirst, newLast,
                                newEmail, newProgram, newYear);
                        summary.studentsUpdated++;
                    }
                }

                String subjectCode = value(row, columns, "subjectcode");
                String subjectName = value(row, columns, "subjectname");
                if (subjectCode.isEmpty() && subjectName.isEmpty()) {
                    continue;
                }

                Subject subject = subjectCode.isEmpty()
                        ? findSubjectByName(subjectName)
                        : markService.getSubject(subjectCode);
                if (subject == null && !subjectName.isEmpty()) {
                    subject = findSubjectByName(subjectName);
                }
                if (subject == null) {
                    summary.addWarning("Line " + lineNumber + ": unknown subject '"
                            + (subjectCode.isEmpty() ? subjectName : subjectCode)
                            + "' - add it under Subjects first. Marks skipped.");
                    continue;
                }

                List<String> categories = new ArrayList<>(subject.getAssessmentWeights().keySet());
                Map<String, Double> assessmentMarks = new LinkedHashMap<>();
                boolean anyMark = false;

                for (int i = 0; i < categories.size(); i++) {
                    Double score = null;
                    if (i < assessmentColumns.size()) {
                        int column = assessmentColumns.get(i);
                        String raw = column < row.length ? row[column].trim() : "";
                        if (!raw.isEmpty()) {
                            try {
                                double parsed = Double.parseDouble(raw);
                                if (parsed < 0 || parsed > 100) {
                                    summary.addWarning("Line " + lineNumber + ": mark '" + raw
                                            + "' for " + categories.get(i)
                                            + " is outside 0-100 - treated as missing.");
                                } else {
                                    score = parsed;
                                    anyMark = true;
                                }
                            } catch (NumberFormatException e) {
                                summary.addWarning("Line " + lineNumber + ": invalid mark '" + raw
                                        + "' for " + categories.get(i) + " - treated as missing.");
                            }
                        }
                    }
                    assessmentMarks.put(categories.get(i), score);
                }

                if (!anyMark) {
                    summary.addWarning("Line " + lineNumber + ": no assessment marks for "
                            + studentId + " in " + subject.getSubjectCode() + " - mark skipped.");
                    continue;
                }

                Mark mark = new Mark(studentId, subject.getSubjectCode(), assessmentMarks);
                try {
                    markService.enterMarks(studentId, subject.getSubjectCode(), mark, "CSV Import");
                } catch (IllegalArgumentException e) {
                    summary.addWarning("Line " + lineNumber + ": " + e.getMessage() + " - mark skipped.");
                    continue;
                }
                // Several rows for the same student/module replace each other, so
                // count the record once rather than once per row.
                if (savedMarks.add(studentId + "|" + subject.getSubjectCode())) {
                    summary.marksSaved++;
                }
            }
            return null;
        });

        return summary;
    }

    /**
     * Exports all marks data to a CSV file that can be edited and re-imported.
     *
     * @param filePath Path to the output CSV file.
     * @return Number of records exported.
     * @throws IOException If the file cannot be written.
     */
    public int exportMarksToCSV(String filePath) throws IOException {
        try {
            return markService.withReadLock(() -> {
                try {
                    return CSVHelper.writeMarksCSV(filePath,
                            markService.getAllStudents(), markService.getAllSubjects());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
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

    private Subject findSubjectByName(String name) {
        String target = normalise(name);
        if (target.isEmpty()) {
            return null;
        }
        for (Subject subject : markService.getAllSubjects()) {
            if (normalise(subject.getSubjectName()).equals(target)) {
                return subject;
            }
        }
        return null;
    }

    private static String value(String[] row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        if (index == null || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index].trim();
    }

    private static Map<String, Integer> headerIndexes(String[] headers) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i] == null ? "" : headers[i];
            header = header.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT)
                    .replace(" ", "").replace("_", "");
            if (!header.isEmpty() && !indexes.containsKey(header)) {
                indexes.put(header, i);
            }
        }
        return indexes;
    }

    private static List<Integer> assessmentColumns(String[] headers) {
        List<Map.Entry<Integer, Integer>> numbered = new ArrayList<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i] == null ? "" : headers[i].trim();
            Matcher matcher = ASSESSMENT_COLUMN.matcher(header);
            if (matcher.matches()) {
                numbered.add(Map.entry(Integer.parseInt(matcher.group(1)), i));
            }
        }
        numbered.sort(Comparator.comparingInt(Map.Entry::getKey));

        List<Integer> columns = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : numbered) {
            columns.add(entry.getValue());
        }
        return columns;
    }

    private static String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /**
     * Result of a CSV import.
     */
    public static class ImportSummary {
        private int rowsProcessed;
        private int studentsCreated;
        private int studentsUpdated;
        private int marksSaved;
        private final List<String> warnings = new ArrayList<>();

        public int getRowsProcessed() {
            return rowsProcessed;
        }

        public int getStudentsCreated() {
            return studentsCreated;
        }

        public int getStudentsUpdated() {
            return studentsUpdated;
        }

        public int getMarksSaved() {
            return marksSaved;
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        void addWarning(String warning) {
            warnings.add(warning);
        }

        /**
         * @return A human-readable summary suitable for the CLI or a dialog.
         */
        public String describe() {
            StringBuilder text = new StringBuilder();
            text.append("Rows processed: ").append(rowsProcessed).append('\n');
            text.append("Students created: ").append(studentsCreated).append('\n');
            text.append("Students updated: ").append(studentsUpdated).append('\n');
            text.append("Marks saved: ").append(marksSaved);

            if (!warnings.isEmpty()) {
                text.append("\n\nWarnings (").append(warnings.size()).append("):\n");
                int shown = Math.min(warnings.size(), 15);
                for (int i = 0; i < shown; i++) {
                    text.append(" - ").append(warnings.get(i)).append('\n');
                }
                if (warnings.size() > shown) {
                    text.append(" - ... and ").append(warnings.size() - shown).append(" more.\n");
                }
            }
            return text.toString();
        }
    }
}
