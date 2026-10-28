package com.group21.marksystem.services;

import com.group21.marksystem.db.Database;
import com.group21.marksystem.db.MarkRepository;
import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.utils.CPUTData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
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
    private final MarkRepository database;

    /**
     * Creates a service backed by the shared SQLite database.
     * Existing data is loaded immediately.
     */
    public MarkService() {
        this(Database.getInstance());
    }

    /**
     * Creates a service backed by the supplied database (used by tests).
     *
     * @param database The database to load from and persist to.
     */
    public MarkService(MarkRepository database) {
        this.database = database;
        this.lock = new ReentrantReadWriteLock();
        this.subjects = new ConcurrentHashMap<>(database.loadSubjects());
        this.students = new ConcurrentHashMap<>(database.loadStudents());
        database.loadMarks(students, subjects);
    }

    /**
     * @return The persistence store used by this service.
     */
    public MarkRepository getDatabase() {
        return database;
    }

    /**
     * Runs an action while holding the service write lock.
     *
     * <p>Service methods acquire the same reentrant lock, so a caller can use
     * this to make a multi-step operation (such as a CSV import) atomic with
     * respect to other readers and writers.</p>
     *
     * @param action The action to run.
     * @param <T>    The returned type.
     * @return The action's result.
     */
    public <T> T withWriteLock(Supplier<T> action) {
        lock.writeLock().lock();
        try {
            return action.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Runs an action while holding the service read lock.
     *
     * @param action The action to run.
     * @param <T>    The returned type.
     * @return The action's result.
     */
    public <T> T withReadLock(Supplier<T> action) {
        lock.readLock().lock();
        try {
            return action.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ========== Student Management ==========

    /**
     * Adds a new student.
     *
     * @param student The student to add.
     * @return true if added successfully, false if student ID already exists.
     * @throws IllegalArgumentException If the student details are invalid.
     */
    public boolean addStudent(Student student) {
        validateStudent(student);
        lock.writeLock().lock();
        try {
            if (students.containsKey(student.getStudentId())) {
                return false;
            }
            database.saveStudent(student);
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
     * @throws IllegalArgumentException If the supplied details are invalid.
     */
    public boolean updateStudent(String studentId, String firstName, String lastName,
                                 String email, String program, int year) {
        requireText(studentId, "Student ID");
        requireText(firstName, "First name");
        requireText(lastName, "Last name");
        requireYear(year);

        lock.writeLock().lock();
        try {
            Student student = students.get(studentId);
            if (student == null) {
                return false;
            }
            student.setFirstName(firstName.trim());
            student.setLastName(lastName.trim());
            student.setEmail(email == null ? "" : email.trim());
            student.setProgram(program == null ? "" : program.trim());
            student.setYear(year);
            database.saveStudent(student);
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
            if (!students.containsKey(studentId)) {
                return false;
            }
            database.deleteStudent(studentId);
            students.remove(studentId);
            return true;
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
            List<Student> result = new ArrayList<>(students.values());
            result.sort(Comparator.comparing(Student::getStudentId));
            return result;
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
        String lowerQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        lock.readLock().lock();
        try {
            return students.values().stream()
                    .filter(s -> containsIgnoreCase(s.getFirstName(), lowerQuery)
                            || containsIgnoreCase(s.getLastName(), lowerQuery)
                            || containsIgnoreCase(s.getStudentId(), lowerQuery))
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
                    .filter(s -> s.getProgram() != null && s.getProgram().equalsIgnoreCase(program))
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
     * @throws IllegalArgumentException If the subject or its weights are invalid.
     */
    public boolean addSubject(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject is required.");
        }
        requireText(subject.getSubjectCode(), "Subject code");
        requireText(subject.getSubjectName(), "Subject name");
        if (subject.getCredits() < 1 || subject.getCredits() > 99) {
            throw new IllegalArgumentException("Credits must be between 1 and 99.");
        }
        validateWeights(subject.getAssessmentWeights());

        lock.writeLock().lock();
        try {
            if (subjects.containsKey(subject.getSubjectCode())) {
                return false;
            }
            database.saveSubject(subject);
            subjects.put(subject.getSubjectCode(), subject);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Updates an existing subject and its assessment weights.
     *
     * <p>Marks are realigned with the new categories: scores for retained
     * categories are kept, new categories are treated as missing and removed
     * categories are dropped. A mark can therefore never be calculated against
     * a category list it no longer belongs to.</p>
     *
     * @param subjectCode The subject to update.
     * @param name        New subject name.
     * @param credits     New credit value.
     * @param description New description.
     * @param weights     New assessment weights (category -> percentage).
     * @return true if the subject exists and was updated.
     * @throws IllegalArgumentException If the new details or weights are invalid.
     */
    public boolean updateSubject(String subjectCode, String name, int credits,
                                 String description, Map<String, Double> weights) {
        requireText(subjectCode, "Subject code");
        requireText(name, "Subject name");
        if (credits < 1 || credits > 99) {
            throw new IllegalArgumentException("Credits must be between 1 and 99.");
        }
        validateWeights(weights);

        lock.writeLock().lock();
        try {
            Subject subject = subjects.get(subjectCode);
            if (subject == null) {
                return false;
            }

            subject.setSubjectName(name.trim());
            subject.setCredits(credits);
            subject.setDescription(description == null ? "" : description.trim());
            subject.setAssessmentWeights(new LinkedHashMap<>(weights));

            // Persist the new category list first, then rewrite each mark so it
            // exactly matches the subject's assessments.
            database.saveSubject(subject);

            for (Student student : students.values()) {
                Mark mark = student.getMark(subjectCode);
                if (mark != null) {
                    mark.alignToCategories(subject.getAssessmentWeights());
                    database.saveMark(mark);
                }
            }
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes a subject by code, including its marks.
     *
     * @param subjectCode The subject code to delete.
     * @return true if the subject was deleted.
     */
    public boolean deleteSubject(String subjectCode) {
        lock.writeLock().lock();
        try {
            if (!subjects.containsKey(subjectCode)) {
                return false;
            }
            database.deleteSubject(subjectCode);
            subjects.remove(subjectCode);
            for (Student student : students.values()) {
                student.getMarks().remove(subjectCode);
            }
            return true;
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
            List<Subject> result = new ArrayList<>(subjects.values());
            result.sort(Comparator.comparing(Subject::getSubjectCode));
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ========== Mark Management ==========

    /**
     * Enters marks for a student in a subject.
     * Calculates weighted totals based on the subject's assessment weights.
     *
     * @param studentId   The student ID.
     * @param subjectCode The subject code.
     * @param mark        The Mark object containing assessment marks.
     * @param enteredBy   The user entering the marks.
     * @return true if marks were entered successfully.
     * @throws IllegalArgumentException If a score is outside 0-100.
     */
    public boolean enterMarks(String studentId, String subjectCode, Mark mark, String enteredBy) {
        if (mark == null) {
            throw new IllegalArgumentException("Mark is required.");
        }
        validateScores(mark);

        lock.writeLock().lock();
        try {
            Student student = students.get(studentId);
            Subject subject = subjects.get(subjectCode);

            if (student == null || subject == null) {
                return false;
            }

            mark.setEnteredBy(enteredBy);
            mark.alignToCategories(subject.getAssessmentWeights());

            database.saveMark(mark);
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
                    result.add(new java.util.AbstractMap.SimpleEntry<>(student, mark));
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

    // ========== Seeding ==========

    /**
     * Initializes CPUT Bellville sample data when the database has never been
     * seeded. Calling this repeatedly is safe: existing data is never
     * overwritten and deleted demo data is not resurrected.
     */
    public void initializeSampleData() {
        seedIfEmpty();
    }

    /**
     * Seeds the CPUT Bellville engineering programmes, class list and a set of
     * example marks the first time this database is used.
     *
     * <p>Once the seed has run, the fact is recorded in the database. Emptying
     * the marks table later (for example by deleting all marks) therefore no
     * longer triggers another demo import.</p>
     */
    public void seedIfEmpty() {
        if (database.isSeeded()) {
            return;
        }

        boolean hadSubjects = !subjects.isEmpty();
        boolean hadStudents = !students.isEmpty();

        if (!hadSubjects) {
            for (Subject subject : CPUTData.subjects()) {
                addSubject(subject);
            }
        }

        if (!hadStudents) {
            for (Student student : CPUTData.students()) {
                addStudent(student);
            }
        }

        // Only invent demo marks for a database that we seeded from scratch.
        if (!hadSubjects && !hadStudents
                && database.isMarkTableEmpty()
                && !students.isEmpty() && !subjects.isEmpty()) {
            for (Mark mark : CPUTData.marks(getAllSubjects(), getAllStudents())) {
                enterMarks(mark.getStudentId(), mark.getSubjectCode(), mark, "Seed data");
            }
        }

        database.markSeeded();
    }

    /**
     * Reloads students, subjects and marks from the database. This makes
     * changes made in another interface (for example the CLI) visible in the
     * GUI without restarting the application.
     */
    public void reloadFromDatabase() {
        lock.writeLock().lock();
        try {
            Map<String, Subject> loadedSubjects = new ConcurrentHashMap<>(database.loadSubjects());
            Map<String, Student> loadedStudents = new ConcurrentHashMap<>(database.loadStudents());
            database.loadMarks(loadedStudents, loadedSubjects);
            this.subjects = loadedSubjects;
            this.students = loadedStudents;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ========== Validation helpers ==========

    private static void validateStudent(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student is required.");
        }
        requireText(student.getStudentId(), "Student ID");
        requireText(student.getFirstName(), "First name");
        requireText(student.getLastName(), "Last name");
        requireYear(student.getYear());
    }

    private static void requireYear(int year) {
        if (year < 1 || year > 10) {
            throw new IllegalArgumentException("Year must be between 1 and 10.");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
    }

    private static void validateWeights(Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Add at least one assessment category.");
        }

        double total = 0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            requireText(entry.getKey(), "Assessment category");
            Double weight = entry.getValue();
            if (weight == null || !Double.isFinite(weight) || weight <= 0 || weight > 100) {
                throw new IllegalArgumentException(
                        "Weight for '" + entry.getKey() + "' must be greater than 0 and at most 100.");
            }
            total += weight;
        }

        if (Math.abs(total - 100.0) > 0.01) {
            throw new IllegalArgumentException(String.format(
                    "Assessment weights must add up to 100%% (currently %.2f%%).", total));
        }
    }

    private static void validateScores(Mark mark) {
        for (Map.Entry<String, Double> entry : mark.getAssessmentMarks().entrySet()) {
            Double score = entry.getValue();
            if (score == null) {
                continue; // missing marks are allowed and flagged
            }
            if (!Double.isFinite(score) || score < 0 || score > 100) {
                throw new IllegalArgumentException(
                        "Mark for '" + entry.getKey() + "' must be between 0 and 100.");
            }
        }
    }

    private static boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }
}
