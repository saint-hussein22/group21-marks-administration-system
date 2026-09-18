package com.group21.marksystem.models;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a subject/course in the Marks Administration System.
 * Contains assessment categories with their weight percentages.
 *
 * @author Group 21
 */
public class Subject implements Serializable {
    private static final long serialVersionUID = 1L;

    private String subjectCode;
    private String subjectName;
    private int credits;
    private String description;
    private Map<String, Double> assessmentWeights; // category -> weight percentage

    public Subject(String subjectCode, String subjectName, int credits, String description) {
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.credits = credits;
        this.description = description;
        this.assessmentWeights = new LinkedHashMap<>();
    }

    // Add an assessment category with its weight percentage
    public void addAssessment(String category, double weight) {
        assessmentWeights.put(category, weight);
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Double> getAssessmentWeights() {
        return assessmentWeights;
    }

    public void setAssessmentWeights(Map<String, Double> assessmentWeights) {
        this.assessmentWeights = assessmentWeights;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%d credits)", subjectCode, subjectName, credits);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Subject subject = (Subject) obj;
        return subjectCode.equals(subject.subjectCode);
    }

    @Override
    public int hashCode() {
        return subjectCode.hashCode();
    }
}
