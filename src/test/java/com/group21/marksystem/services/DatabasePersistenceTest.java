package com.group21.marksystem.services;

import com.group21.marksystem.db.Database;
import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.models.User;
import com.group21.marksystem.utils.CPUTData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the CLI/GUI data services share one SQLite database.
 */
class DatabasePersistenceTest {

    @TempDir
    Path tempDir;

    private Database database;
    private MarkService markService;
    private String studentId;
    private String secondStudentId;

    @BeforeEach
    void setUp() {
        database = new Database(tempDir.resolve("marks.db").toString());
        markService = new MarkService(database);
        markService.seedIfEmpty();

        List<Student> seeded = markService.getAllStudents();
        studentId = seeded.get(0).getStudentId();
        secondStudentId = seeded.get(1).getStudentId();
    }

    @Test
    void seedsCPUTEngineeringDataWithRandomStudentNumbers() {
        assertFalse(markService.getAllSubjects().isEmpty());
        assertEquals(23, markService.getAllStudents().size());
        assertEquals(23, CPUTData.randomStudentNumbers(23).stream().distinct().count());

        assertNotNull(markService.getStudent(CPUTData.FIRST_STUDENT_NUMBER));
        assertTrue(CPUTData.FIRST_STUDENT_NUMBER.matches("\\d{9}"));
        assertEquals("25", CPUTData.FIRST_STUDENT_NUMBER.substring(0, 2));
        assertTrue(studentId.matches("\\d{9}"));
        assertTrue(markService.getStudent(studentId).getMarks().size() > 0);
        assertTrue(markService.getAllSubjects().size() >= 25);
    }

    @Test
    void studentEditsSurviveReload() {
        String email = studentId + "@mycput.ac.za";
        assertTrue(markService.updateStudent(studentId, "Hussein", "Akim",
                email, CPUTData.CIVIL, 3));

        MarkService reloaded = new MarkService(database);
        Student student = reloaded.getStudent(studentId);
        assertNotNull(student);
        assertEquals(3, student.getYear());
        assertEquals(email, student.getEmail());
    }

    @Test
    void marksSurviveReloadAndRecalculateFromSubjectWeights() {
        Map<String, Double> marks = new LinkedHashMap<>();
        marks.put("Drawing", 80.0);
        marks.put("Practical", 70.0);
        marks.put("Exam", 90.0);

        Mark mark = new Mark(studentId, "CIV150S", marks);
        assertTrue(markService.enterMarks(studentId, "CIV150S", mark, "lecturer1"));

        MarkService reloaded = new MarkService(database);
        Mark stored = reloaded.getMarks(studentId, "CIV150S");
        assertNotNull(stored);
        assertEquals(82.0, stored.getWeightedTotal(), 0.001);
        assertEquals(Mark.Grade.B_PLUS, stored.getLetterGrade());
        assertEquals("lecturer1", stored.getEnteredBy());
    }

    @Test
    void deletingStudentRemovesMarks() {
        assertTrue(markService.deleteStudent(secondStudentId));
        assertNull(markService.getStudent(secondStudentId));

        MarkService reloaded = new MarkService(database);
        assertNull(reloaded.getStudent(secondStudentId));
        assertFalse(database.isStudentTableEmpty());
    }

    @Test
    void subjectEditsSurviveReload() {
        Subject subject = markService.getSubject("CIV260S");
        assertNotNull(subject);

        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("Test 1", 30.0);
        weights.put("Test 2", 30.0);
        weights.put("Exam", 40.0);

        assertTrue(markService.updateSubject("CIV260S", "Structural Analysis II", 14,
                "Civil Engineering - Year 2", weights));

        MarkService reloaded = new MarkService(database);
        Subject stored = reloaded.getSubject("CIV260S");
        assertNotNull(stored);
        assertEquals("Structural Analysis II", stored.getSubjectName());
        assertEquals(14, stored.getCredits());
        assertEquals(weights, stored.getAssessmentWeights());
    }

    @Test
    void usersAreStoredAndAuthenticateAfterReload() {
        UserService userService = new UserService(database);
        userService.initializeDefaultUsers();

        assertNotNull(userService.registerUser("lecturer9", "secret123",
                User.Role.LECTURER, "Dr. Nine", "Civil Engineering"));

        UserService reloaded = new UserService(database);
        User user = reloaded.authenticate("lecturer9", "secret123");
        assertNotNull(user);
        assertEquals("Dr. Nine", user.getFullName());

        assertTrue(reloaded.updateUser("lecturer9", "newpass", User.Role.HOD,
                "Prof. Nine", "Faculty of Engineering"));
        UserService afterUpdate = new UserService(database);
        assertNull(afterUpdate.authenticate("lecturer9", "secret123"));
        assertNotNull(afterUpdate.authenticate("lecturer9", "newpass"));
        assertTrue(afterUpdate.deleteUser("lecturer9"));
        assertNull(new UserService(database).getUser("lecturer9"));
    }
}
