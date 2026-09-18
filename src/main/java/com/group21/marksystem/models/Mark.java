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

    // Calculate total and weighted marks
    private void calculateTotals() {
        double rawSum = 0;
        double weightedSum = 0;
        double totalWeight = 0;

        for (Map.Entry<String, Double> entry : assessmentMarks.entrySet()) {
            String category = entry.getKey();
            Double mark = entry.getValue();
            if (mark != null) {
                rawSum += mark;
                // Weighted calculation would require subject weights
                // This will be set by the service layer
            } else {
                isMissing = true;
            }
        }

        this.totalMark = rawSum;
        this.weightedTotal = weightedSum;
        this.letterGrade = determineGrade(rawSum);
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

    // Calculate weighted total using subject's assessment weights
    public void calculateWeightedTotal(Map<String, Double> weights) {
        double sum = 0;
        double totalWeight = 0;

        for (Map.Entry<String, Double> entry : assessmentMarks.entrySet()) {
            String category = entry.getKey();
            Double mark = entry.getValue();
            Double weight = weights.get(category);

            if (mark != null && weight != null) {
                sum += mark * (weight / 100.0);
                totalWeight += weight;
            }
        }

        this.weightedTotal = totalWeight > 0 ? (sum / totalWeight) * 100 : 0;
        this.letterGrade = determineGrade(this.weightedTotal);
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
        return String.format("Mark{student=%s, subject=%s, total=%.2f, grade=%s, missing=%s}",
                studentId, subjectCode, weightedTotal > 0 ? weightedTotal : totalMark,
                letterGrade, isMissing);
    }
}
