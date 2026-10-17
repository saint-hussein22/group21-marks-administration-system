package com.group21.marksystem.services;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Service class for managing students, subjects, and marks.
 * Thread-safe using read-write locks and concurrent collections.
 *
 * @author Group 21
 */
public class MarkService {
    private Map<String, Student> students; // studentId -> Student
    private Map<String, Subject> subjects; // subjectCode -> Subject
    private ReadWriteLock lock;

    public MarkService() {
        this.students = new ConcurrentHashMap<>();
        this.subjects = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    // ========== Student Management ==========

    /**
     * Adds a new student.
     *
     * @param student The student to add.
     * @return true if added successfully, false if student ID already exists.
     */
    public boolean addStudent(Student student) {
        lock.writeLock().lock();
        try {
            if (students.containsKey(student.getStudentId())) {
                return false;
            }
            students.put(student.getStudentId(), student);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Updates an existing student's information.
     *
     * @param studentId The student ID to update.
     * @param firstName Updated first name.
     * @param lastName  Updated last name.
     * @param email     Updated email.
     * @param program   Updated program.
     * @param year      Updated year.
     * @return true if update was successful.
     */
    public boolean updateStudent(String studentId, String firstName, String lastName,
                                 String email, String program, int year) {
        lock.writeLock().lock();
        try {
            Student student = students.get(studentId);
            if (student == null) {
                return false;
            }
            student.setFirstName(firstName);
            student.setLastName(lastName);
            student.setEmail(email);
            student.setProgram(program);
            student.setYear(year);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes a student by ID.
     *
     * @param studentId The student ID to delete.
     * @return true if the student was deleted.
     */
    public boolean deleteStudent(String studentId) {
        lock.writeLock().lock();
        try {
            return students.remove(studentId) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a student by ID.
     *
     * @param studentId The student ID.
     * @return The Student, or null if not found.
     */
    public Student getStudent(String studentId) {
        lock.readLock().lock();
        try {
            return students.get(studentId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns all students.
     *
     * @return List of all students.
     */
    public List<Student> getAllStudents() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(students.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Searches students by name (case-insensitive).
     *
     * @param query The search term.
     * @return List of matching students.
     */
    public List<Student> searchStudents(String query) {
        lock.readLock().lock();
        try {
            String lowerQuery = query.toLowerCase();
            return students.values().stream()
                    .filter(s -> s.getFirstName().toLowerCase().contains(lowerQuery) ||
                            s.getLastName().toLowerCase().contains(lowerQuery) ||
                            s.getStudentId().toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns all students in a specific program.
     *
     * @param program The program name.
     * @return List of students in the program.
     */
    public List<Student> getStudentsByProgram(String program) {
        lock.readLock().lock();
        try {
            return students.values().stream()
                    .filter(s -> s.getProgram().equalsIgnoreCase(program))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    // ========== Subject Management ==========

    /**
     * Adds a new subject.
     *
     * @param subject The subject to add.
     * @return true if added successfully, false if subject code already exists.
     */
    public boolean addSubject(Subject subject) {
        lock.writeLock().lock();
        try {
            if (subjects.containsKey(subject.getSubjectCode())) {
                return false;
            }
            subjects.put(subject.getSubjectCode(), subject);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes a subject by code.
     *
     * @param subjectCode The subject code to delete.
     * @return true if the subject was deleted.
     */
    public boolean deleteSubject(String subjectCode) {
        lock.writeLock().lock();
        try {
            return subjects.remove(subjectCode) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a subject by code.
     *
     * @param subjectCode The subject code.
     * @return The Subject, or null if not found.
     */
    public Subject getSubject(String subjectCode) {
        lock.readLock().lock();
        try {
            return subjects.get(subjectCode);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns all subjects.
     *
     * @return List of all subjects.
     */
    public List<Subject> getAllSubjects() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(subjects.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clears all students and subjects from memory.
     */
    public void clearAllData() {
        lock.writeLock().lock();
        try {
            students.clear();
            subjects.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ========== Mark Management ==========

    /**
     * Enters marks for a student in a subject.
     * Calculates weighted totals based on the subject's assessment weights.
     *
     * @param studentId     The student ID.
     * @param subjectCode   The subject code.
     * @param mark          The Mark object containing assessment marks.
     * @param enteredBy     The user entering the marks.
     * @return true if marks were entered successfully.
     */
    public boolean enterMarks(String studentId, String subjectCode, Mark mark, String enteredBy) {
        lock.writeLock().lock();
        try {
            Student student = students.get(studentId);
            Subject subject = subjects.get(subjectCode);

            if (student == null || subject == null) {
                return false;
            }

            mark.setEnteredBy(enteredBy);

            // Calculate weighted total using subject's assessment weights
            if (!mark.getAssessmentMarks().isEmpty()) {
                mark.calculateWeightedTotal(subject.getAssessmentWeights());
            }

            student.addMark(subjectCode, mark);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a student's marks for a specific subject.
     *
     * @param studentId   The student ID.
     * @param subjectCode The subject code.
     * @return The Mark, or null if not found.
     */
    public Mark getMarks(String studentId, String subjectCode) {
        lock.readLock().lock();
        try {
            Student student = students.get(studentId);
            if (student == null) return null;
            return student.getMark(subjectCode);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns all students with missing marks for a subject.
     *
     * @param subjectCode The subject code.
     * @return List of students with missing marks, paired with their marks (if any).
     */
    public List<Map.Entry<Student, Mark>> getStudentsWithMissingMarks(String subjectCode) {
        lock.readLock().lock();
        try {
            List<Map.Entry<Student, Mark>> result = new ArrayList<>();

            for (Student student : students.values()) {
                Mark mark = student.getMark(subjectCode);
                if (mark == null || mark.isMissing()) {
                    result.add(new AbstractMap.SimpleEntry<>(student, mark));
                }
            }

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ========== Grade Calculation ==========

    /**
     * Calculates the GPA for a student.
     *
     * @param studentId The student ID.
     * @return The GPA (0.0 to 4.0 scale).
     */
    public double calculateGPA(String studentId) {
        lock.readLock().lock();
        try {
            Student student = students.get(studentId);
            if (student == null || student.getMarks().isEmpty()) {
                return 0.0;
            }

            double totalPoints = 0;
            int totalCredits = 0;

            for (Map.Entry<String, Mark> entry : student.getMarks().entrySet()) {
                Subject subject = subjects.get(entry.getKey());
                Mark mark = entry.getValue();

                if (subject != null && mark != null && !mark.isMissing()) {
                    double gradePoints = convertGradeToPoints(mark.getLetterGrade());
                    totalPoints += gradePoints * subject.getCredits();
                    totalCredits += subject.getCredits();
                }
            }

            return totalCredits > 0 ? totalPoints / totalCredits : 0.0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Converts a letter grade to grade points.
     *
     * @param grade The letter grade.
     * @return Grade points on a 4.0 scale.
     */
    private double convertGradeToPoints(Mark.Grade grade) {
        switch (grade) {
            case A_PLUS: return 4.0;
            case A:  return 4.0;
            case B_PLUS: return 3.5;
            case B:  return 3.0;
            case C_PLUS: return 2.5;
            case C:  return 2.0;
            case D:  return 1.0;
            case F:  return 0.0;
            default: return 0.0;
        }
    }

    // ========== Persistence ==========

    /**
     * Serializes all data (students and subjects) to a file.
     *
     * @param filePath The file path to save to.
     */
    public void saveToFile(String filePath) throws IOException {
        lock.readLock().lock();
        try {
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(Paths.get(filePath))))) {
                oos.writeObject(new HashMap<>(students));
                oos.writeObject(new HashMap<>(subjects));
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Loads all data from a serialized file.
     *
     * @param filePath The file path to load from.
     */
    @SuppressWarnings("unchecked")
    public void loadFromFile(String filePath) throws IOException, ClassNotFoundException {
        lock.writeLock().lock();
        try {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(Files.newInputStream(Paths.get(filePath))))) {
                students = (HashMap<String, Student>) ois.readObject();
                subjects = (HashMap<String, Subject>) ois.readObject();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Initializes sample data for testing/demo purposes.
     */
    public void initializeSampleData() {
        // Create sample subjects
        Subject cs260 = new Subject("CS260", "Software Design II", 6,
                "Advanced software design principles and practices");
        cs260.addAssessment("Assignment 1", 20);
        cs260.addAssessment("Assignment 2", 20);
        cs260.addAssessment("Exam", 60);
        addSubject(cs260);

        Subject prg300 = new Subject("PRG300", "Programming Fundamentals", 8,
                "Introduction to programming concepts");
        prg300.addAssessment("Lab Work", 30);
        prg300.addAssessment("Project", 30);
        prg300.addAssessment("Exam", 40);
        addSubject(prg300);

        Subject db320 = new Subject("DB320", "Database Systems", 6,
                "Database design and management");
        db320.addAssessment("Assignment 1", 25);
        db320.addAssessment("Assignment 2", 25);
        db320.addAssessment("Exam", 50);
        addSubject(db320);

        // Create sample students
        addStudent(new Student("S001", "Alice", "Johnson",
                "alice.johnson@student.university.ac.za", "Computer Science", 2));
        addStudent(new Student("S002", "Bob", "Smith",
                "bob.smith@student.university.ac.za", "Computer Science", 2));
        addStudent(new Student("S003", "Carol", "Williams",
                "carol.williams@student.university.ac.za", "Computer Science", 2));
        addStudent(new Student("S004", "David", "Brown",
                "david.brown@student.university.ac.za", "Information Technology", 2));
        addStudent(new Student("S005", "Eve", "Davis",
                "eve.davis@student.university.ac.za", "Computer Science", 2));
    }
}
