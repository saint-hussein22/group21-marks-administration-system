package com.group21.marksystem.db;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.models.User;

import java.util.Map;

/**
 * Persistence contract for students, subjects, marks and users.
 *
 * <p>Services depend on this interface rather than the SQLite {@link Database}
 * class, so another store can replace the database without changing callers.</p>
 *
 * @author Group 21
 */
public interface MarkRepository {

    /**
     * @return Absolute path of the store, for display in the CLI and GUI.
     */
    String getPath();

    /**
     * Inserts a student or updates the existing row with the same ID.
     *
     * @param student The student to persist.
     */
    void saveStudent(Student student);

    /**
     * Deletes a student and that student's marks.
     *
     * @param studentId The student ID.
     */
    void deleteStudent(String studentId);

    /**
     * Loads every student.
     *
     * @return Map of student ID to student.
     */
    Map<String, Student> loadStudents();

    /**
     * Inserts a subject or replaces its details and assessment weights.
     *
     * @param subject The subject to persist.
     */
    void saveSubject(Subject subject);

    /**
     * Deletes a subject, its assessment weights and its marks.
     *
     * @param subjectCode The subject code.
     */
    void deleteSubject(String subjectCode);

    /**
     * Loads every subject, including assessment weights.
     *
     * @return Map of subject code to subject.
     */
    Map<String, Subject> loadSubjects();

    /**
     * Replaces every assessment score for one student and subject.
     *
     * @param mark The mark to persist.
     */
    void saveMark(Mark mark);

    /**
     * Loads marks into the supplied students, using the supplied subjects for weights.
     *
     * @param students Already loaded students.
     * @param subjects Already loaded subjects.
     */
    void loadMarks(Map<String, Student> students, Map<String, Subject> subjects);

    /**
     * Inserts a user or updates the existing account.
     *
     * @param user The user to persist.
     */
    void saveUser(User user);

    /**
     * Deletes a user account.
     *
     * @param username The username.
     */
    void deleteUser(String username);

    /**
     * Loads every user.
     *
     * @return Map of username to user.
     */
    Map<String, User> loadUsers();

    /**
     * @return true once demo data has been seeded into this store.
     */
    boolean isSeeded();

    /**
     * Records that demo data has been seeded.
     */
    void markSeeded();

    /**
     * @return true when the student table has no rows.
     */
    boolean isStudentTableEmpty();

    /**
     * @return true when the subject table has no rows.
     */
    boolean isSubjectTableEmpty();

    /**
     * @return true when the mark table has no rows.
     */
    boolean isMarkTableEmpty();

    /**
     * @return true when the user table has no rows.
     */
    boolean isUserTableEmpty();
}
