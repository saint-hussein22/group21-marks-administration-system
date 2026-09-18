package com.group21.marksystem.models;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a student in the Marks Administration System.
 * Stores student details and their marks for various subjects.
 *
 * @author Group 21
 */
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String program;
    private int year;
    private Map<String, Mark> marks; // subjectCode -> Mark

    public Student(String studentId, String firstName, String lastName, String email,
                   String program, int year) {
        this.studentId = studentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.program = program;
        this.year = year;
        this.marks = new LinkedHashMap<>();
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Map<String, Mark> getMarks() {
        return marks;
    }

    public void setMarks(Map<String, Mark> marks) {
        this.marks = marks;
    }

    public void addMark(String subjectCode, Mark mark) {
        marks.put(subjectCode, mark);
    }

    public Mark getMark(String subjectCode) {
        return marks.get(subjectCode);
    }

    public boolean hasMissingMarks() {
        return marks.containsValue(null) || marks.values().stream().anyMatch(m -> m == null);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%s)", studentId, getFullName(), program);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Student student = (Student) obj;
        return studentId.equals(student.studentId);
    }

    @Override
    public int hashCode() {
        return studentId.hashCode();
    }
}
