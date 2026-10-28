package com.group21.marksystem.services;

import com.group21.marksystem.db.Database;
import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.utils.CPUTData;
import com.group21.marksystem.utils.CSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the lecturer CSV workflow end to end.
 */
class CSVServiceTest {

    @TempDir
    Path tempDir;

    private Database database;
    private MarkService markService;
    private CSVService csvService;

    @BeforeEach
    void setUp() {
        database = new Database(tempDir.resolve("marks.db").toString());
        markService = new MarkService(database);
        markService.seedIfEmpty();
        csvService = new CSVService(markService);
    }

    @Test
    void templateContainsClassListAndAssessmentColumns() throws IOException {
        Path template = tempDir.resolve("template.csv");
        int rows = csvService.generateTemplate(template.toString());

        assertTrue(Files.exists(template));
        assertEquals(markService.getAllStudents().size(), rows);

        List<String[]> data = CSVHelper.readCSV(template.toString());
        assertEquals("StudentID", data.get(0)[0]);
        assertEquals("SubjectCode", data.get(0)[6]);
        assertEquals("Assessment1", data.get(0)[8]);
        assertTrue(data.get(1)[0].matches("\\d{9}"));
        assertEquals("25", data.get(1)[0].substring(0, 2));
        assertEquals("", data.get(1)[8]);
        assertNotNull(markService.getStudent(CPUTData.FIRST_STUDENT_NUMBER));
    }

    @Test
    void importCreatesStudentsUpdatesExistingAndImportsMarks() throws IOException {
        Student existing = markService.getAllStudents().get(0);
        String existingId = existing.getStudentId();
        String newId = unusedStudentNumber();

        Path csv = tempDir.resolve("marks.csv");
        Files.writeString(csv, String.join("\n",
                "StudentID,FirstName,LastName,Email,Program,Year,SubjectCode,SubjectName,Assessment1,Assessment2,Assessment3",
                String.join(",", existingId, existing.getFirstName(), existing.getLastName(),
                        "updated@mycput.ac.za", CPUTData.CIVIL, "3", "CIV150S",
                        "Civil Engineering 1", "80", "70", "90"),
                String.join(",", newId, "New", "Student", newId + "@mycput.ac.za",
                        CPUTData.CIVIL, "1", "CIV150S", "Civil Engineering 1", "60", "65", "70"),
                String.join(",", existingId, existing.getFirstName(), existing.getLastName(),
                        "updated@mycput.ac.za", CPUTData.CIVIL, "3", "UNKNOWN",
                        "Unknown Module", "50", "50", "50")
        ), StandardCharsets.UTF_8);

        CSVService.ImportSummary summary = csvService.importMarksFromCSVDetailed(csv.toString());

        assertEquals(3, summary.getRowsProcessed());
        assertEquals(1, summary.getStudentsCreated());
        assertEquals(1, summary.getStudentsUpdated());
        assertEquals(2, summary.getMarksSaved());
        assertTrue(summary.hasWarnings());

        Student updated = markService.getStudent(existingId);
        assertEquals(3, updated.getYear());
        assertEquals("updated@mycput.ac.za", updated.getEmail());

        Mark imported = markService.getMarks(existingId, "CIV150S");
        assertNotNull(imported);
        assertEquals(82.0, imported.getWeightedTotal(), 0.001);
        assertEquals("CSV Import", imported.getEnteredBy());

        Student created = markService.getStudent(newId);
        assertNotNull(created);
        assertEquals("New", created.getFirstName());

        MarkService reloaded = new MarkService(database);
        assertEquals(82.0, reloaded.getMarks(existingId, "CIV150S").getWeightedTotal(), 0.001);
        assertNotNull(reloaded.getStudent(newId));
    }

    @Test
    void exportCanBeReimportedWithoutLosingMarks() throws IOException {
        Path export = tempDir.resolve("export.csv");
        int exported = csvService.exportMarksToCSV(export.toString());
        assertTrue(exported > 0);

        List<String[]> rows = CSVHelper.readCSV(export.toString());
        String[] header = rows.get(0);
        assertTrue(header[8].startsWith("Assessment"));
        assertEquals("Grade", header[header.length - 1]);

        int marksBefore = totalMarks(markService);
        CSVService.ImportSummary summary = csvService.importMarksFromCSVDetailed(export.toString());
        assertTrue(summary.getMarksSaved() > 0);
        assertEquals(marksBefore, totalMarks(markService));

        MarkService reloaded = new MarkService(database);
        assertEquals(marksBefore, totalMarks(reloaded));
    }

    @Test
    void importRejectsFilesWithoutStudentIdColumn() throws IOException {
        Path csv = tempDir.resolve("bad.csv");
        Files.writeString(csv, "Name,Assessment1\nAlice,80\n", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> csvService.importMarksFromCSV(csv.toString()));
    }

    private String unusedStudentNumber() {
        long candidate = 259_999_999L;
        while (markService.getStudent(String.valueOf(candidate)) != null) {
            candidate--;
        }
        return String.valueOf(candidate);
    }

    private int totalMarks(MarkService service) {
        int total = 0;
        for (Student student : service.getAllStudents()) {
            total += student.getMarks().size();
        }
        return total;
    }
}
