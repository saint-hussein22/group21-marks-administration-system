package com.group21.marksystem.models;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for mark weighting and missing-mark handling.
 */
class MarkTest {

    private static Map<String, Double> weights() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("Test 1", 25.0);
        weights.put("Test 2", 25.0);
        weights.put("Exam", 50.0);
        return weights;
    }

    private static Map<String, Double> scores(Double test1, Double test2, Double exam) {
        Map<String, Double> marks = new LinkedHashMap<>();
        marks.put("Test 1", test1);
        marks.put("Test 2", test2);
        marks.put("Exam", exam);
        return marks;
    }

    @Test
    void completeMarkUsesSubjectWeights() {
        Mark mark = new Mark("1", "CIV260S", scores(80.0, 70.0, 90.0));
        mark.calculateWeightedTotal(weights());

        assertEquals(82.5, mark.getWeightedTotal(), 0.001);
        assertFalse(mark.isMissing());
        assertEquals(Mark.Grade.B_PLUS, mark.getLetterGrade());
    }

    @Test
    void missingAssessmentCountsAsZeroInsteadOfBeingInflated() {
        Mark mark = new Mark("1", "CIV260S", scores(80.0, 70.0, null));
        mark.calculateWeightedTotal(weights());

        // 80*25% + 70*25% = 37.5 out of 100, not 75 (which normalising over
        // only the entered assessments would produce).
        assertEquals(37.5, mark.getWeightedTotal(), 0.001);
        assertTrue(mark.isMissing());
        assertEquals(Mark.Grade.F, mark.getLetterGrade());
    }

    @Test
    void alignToCategoriesDropsRenamedAndAddsMissingCategories() {
        Mark mark = new Mark("1", "CIV260S", scores(80.0, 70.0, 90.0));

        Map<String, Double> renamed = new LinkedHashMap<>();
        renamed.put("Test 1", 25.0);
        renamed.put("Test 2", 25.0);
        renamed.put("Final Exam", 50.0);
        mark.alignToCategories(renamed);

        assertEquals(List.of("Test 1", "Test 2", "Final Exam"),
                new ArrayList<>(mark.getAssessmentMarks().keySet()));
        assertNull(mark.getAssessmentMarks().get("Final Exam"));
        assertFalse(mark.getAssessmentMarks().containsKey("Exam"));
        assertTrue(mark.isMissing());
        assertEquals(37.5, mark.getWeightedTotal(), 0.001);
    }

    @Test
    void missingFlagIsClearedWhenAllAssessmentsAreEntered() {
        Mark mark = new Mark("1", "CIV260S", scores(80.0, 70.0, null));
        assertTrue(mark.isMissing());

        mark.setAssessmentMarks(scores(80.0, 70.0, 90.0));

        assertFalse(mark.isMissing());
    }

    @Test
    void studentReportsIncompleteMarks() {
        Student student = new Student("1", "Test", "Student", "t@example.com", "Program", 1);

        student.addMark("CIV260S", new Mark("1", "CIV260S", scores(80.0, 70.0, null)));
        assertTrue(student.hasMissingMarks());

        student.addMark("CIV150S", new Mark("1", "CIV150S", scores(80.0, 70.0, 90.0)));
        assertTrue(student.hasMissingMarks());

        student.getMarks().clear();
        student.addMark("CIV150S", new Mark("1", "CIV150S", scores(80.0, 70.0, 90.0)));
        assertFalse(student.hasMissingMarks());
    }
}
