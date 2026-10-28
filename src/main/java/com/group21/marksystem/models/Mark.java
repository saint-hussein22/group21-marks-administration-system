package com.group21.marksystem.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a set of marks for a student in a specific subject.
 * Contains marks for each assessment category and computed totals.
 *
 * @author Group 21
 */
public class Mark implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String subjectCode;
    private Map<String, Double> assessmentMarks; // category -> mark
    private double totalMark;
    private double weightedTotal;
    private Grade letterGrade;
    private LocalDateTime enteredAt;
    private String enteredBy;
    private boolean isMissing; // flag for missing entries

    public enum Grade {
        A_PLUS(90),
        A(85),
        B_PLUS(80),
        B(75),
        C_PLUS(70),
        C(65),
        D(60),
        F(0);

        private final int threshold;

        Grade(int threshold) {
            this.threshold = threshold;
        }

        public int getThreshold() {
            return threshold;
        }

        @Override
        public String toString() {
            return name().replace("_PLUS", "+");
        }
    }

    public Mark(String studentId, String subjectCode, Map<String, Double> assessmentMarks) {
        this.studentId = studentId;
        this.subjectCode = subjectCode;
        this.assessmentMarks = assessmentMarks;
        this.enteredAt = LocalDateTime.now();
        this.isMissing = false;
        calculateTotals();
    }

    /**
     * Recalculates the raw total, the missing flag and an unweighted average
     * grade. Subject weights are not known at construction time, so
     * {@link #calculateWeightedTotal(Map)} (or {@link #alignToCategories(Map)})
     * must be called before a weighted grade is meaningful.
     */
    private void calculateTotals() {
        double rawSum = 0;
        int scored = 0;
        boolean missing = false;

        for (Double mark : assessmentMarks.values()) {
            if (mark == null || !Double.isFinite(mark)) {
                missing = true;
            } else {
                rawSum += mark;
                scored++;
            }
        }

        this.totalMark = rawSum;
        this.weightedTotal = 0;
        this.isMissing = missing;
        // Without weights, grade on the average percentage of the entered marks.
        this.letterGrade = determineGrade(scored > 0 ? rawSum / scored : 0);
    }

    // Determine letter grade from numeric total
    private Grade determineGrade(double total) {
        for (Grade grade : Grade.values()) {
            if (total >= grade.getThreshold()) {
                return grade;
            }
        }
        return Grade.F;
    }

    /**
     * Calculates the weighted total as a percentage of the subject's full
     * weight. A missing or absent assessment counts as zero and flags the mark
     * as incomplete, so an incomplete record can never be inflated by
     * normalising only over the assessments that happen to be present.
     *
     * @param weights The subject's assessment weights (category -> percentage).
     */
    public void calculateWeightedTotal(Map<String, Double> weights) {
        double earned = 0;
        double totalWeight = 0;
        boolean missing = false;

        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            Double weight = entry.getValue();
            if (weight == null || !Double.isFinite(weight) || weight <= 0) {
                continue;
            }
            totalWeight += weight;

            Double mark = assessmentMarks.get(entry.getKey());
            if (mark == null || !Double.isFinite(mark)) {
                missing = true;
            } else {
                earned += mark * (weight / 100.0);
            }
        }

        this.weightedTotal = totalWeight > 0 ? (earned / totalWeight) * 100 : 0;
        this.isMissing = missing;
        this.letterGrade = determineGrade(this.weightedTotal);
    }

    /**
     * Rebuilds the assessment map so it contains exactly the subject's current
     * categories. Scores for retained categories are kept, new categories are
     * recorded as missing and removed categories are dropped. This keeps marks
     * consistent when a subject's assessments are edited.
     *
     * @param weights The subject's current assessment weights.
     */
    public void alignToCategories(Map<String, Double> weights) {
        Map<String, Double> aligned = new java.util.LinkedHashMap<>();
        for (String category : weights.keySet()) {
            aligned.put(category, assessmentMarks.get(category));
        }
        this.assessmentMarks = aligned;
        calculateTotals();
        calculateWeightedTotal(weights);
    }

    public String getStudentId() {
        return studentId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public Map<String, Double> getAssessmentMarks() {
        return assessmentMarks;
    }

    public void setAssessmentMarks(Map<String, Double> assessmentMarks) {
        this.assessmentMarks = assessmentMarks;
        calculateTotals();
    }

    public double getTotalMark() {
        return totalMark;
    }

    public void setTotalMark(double totalMark) {
        this.totalMark = totalMark;
    }

    public double getWeightedTotal() {
        return weightedTotal;
    }

    public Grade getLetterGrade() {
        return letterGrade;
    }

    public LocalDateTime getEnteredAt() {
        return enteredAt;
    }

    public void setEnteredAt(LocalDateTime enteredAt) {
        this.enteredAt = enteredAt;
    }

    public String getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(String enteredBy) {
        this.enteredBy = enteredBy;
    }

    public boolean isMissing() {
        return isMissing;
    }

    public void setMissing(boolean missing) {
        isMissing = missing;
    }

    @Override
    public String toString() {
        return String.format("Mark{student=%s, subject=%s, score=%.2f, grade=%s, missing=%s}",
                studentId, subjectCode, weightedTotal, letterGrade, isMissing);
    }
}
