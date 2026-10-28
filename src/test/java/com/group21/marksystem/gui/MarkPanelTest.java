package com.group21.marksystem.gui;

import com.group21.marksystem.db.Database;
import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.models.User;
import com.group21.marksystem.services.CSVService;
import com.group21.marksystem.services.MarkService;
import com.group21.marksystem.services.ReportService;
import com.group21.marksystem.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that the marks entry form loads a student's existing marks instead
 * of showing blank fields that would overwrite them on save.
 */
class MarkPanelTest {

    @TempDir
    Path tempDir;

    private MarkService markService;
    private ReportService reportService;
    private CSVService csvService;
    private User user;

    @BeforeEach
    void setUp() {
        Database database = new Database(tempDir.resolve("marks.db").toString());
        markService = new MarkService(database);
        markService.seedIfEmpty();
        UserService userService = new UserService(database);
        reportService = new ReportService(markService, userService);
        csvService = new CSVService(markService);
        user = new User("lecturer1", "hash", User.Role.LECTURER, "Lecturer One", "Engineering");
    }

    @Test
    void selectingAStudentLoadsTheirExistingMarks() {
        Student target = null;
        Subject targetSubject = null;
        Mark targetMark = null;

        outer:
        for (Student student : markService.getAllStudents()) {
            for (Subject subject : markService.getAllSubjects()) {
                Mark mark = student.getMark(subject.getSubjectCode());
                if (mark != null) {
                    target = student;
                    targetSubject = subject;
                    targetMark = mark;
                    break outer;
                }
            }
        }

        assertNotNull(target, "seeded data should contain at least one mark");
        assertNotNull(targetSubject);

        MarkPanel panel = new MarkPanel(markService, reportService, csvService, user);

        List<JComboBox> combos = findAll(panel, JComboBox.class);
        assertEquals(2, combos.size());
        combos.get(0).setSelectedItem(targetSubject);
        combos.get(1).setSelectedItem(target);

        List<JTextField> fields = findAll(panel, JTextField.class);
        List<String> categories = new ArrayList<>(targetSubject.getAssessmentWeights().keySet());
        assertEquals(categories.size(), fields.size());

        for (int i = 0; i < categories.size(); i++) {
            Double expected = targetMark.getAssessmentMarks().get(categories.get(i));
            String actual = fields.get(i).getText();
            if (expected == null) {
                assertEquals("", actual, "missing mark should leave the field empty");
            } else {
                assertEquals(expected.doubleValue(), Double.parseDouble(actual), 0.001);
            }
        }
    }

    private static <T> List<T> findAll(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                result.add(type.cast(component));
            }
            if (component instanceof Container) {
                result.addAll(findAll((Container) component, type));
            }
        }
        return result;
    }
}
