package com.group21.marksystem.utils;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Object-stream backup of students, subjects and marks.
 *
 * <p>SQLite is the live store. This helper demonstrates Java object serialization
 * by writing a portable {@code .ser} snapshot that can be inspected or restored
 * for demos and disaster recovery.</p>
 *
 * @author Group 21
 */
public final class SnapshotHelper {

    private SnapshotHelper() {
    }

    /**
     * Writes the current in-memory domain objects to a binary snapshot file.
     *
     * @param filePath Destination {@code .ser} path.
     * @param students Students to include.
     * @param subjects Subjects to include.
     * @throws IOException If the file cannot be written.
     */
    public static void save(String filePath, List<Student> students, List<Subject> subjects)
            throws IOException {
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Snapshot snapshot = Snapshot.from(students, subjects);
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(path))) {
            out.writeObject(snapshot);
        }
    }

    /**
     * Reads a snapshot previously written by {@link #save}.
     *
     * @param filePath Path to the {@code .ser} file.
     * @return The loaded snapshot.
     * @throws IOException            If the file cannot be read.
     * @throws ClassNotFoundException If the file is not a valid snapshot.
     */
    public static Snapshot load(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(Paths.get(filePath)))) {
            Object raw = in.readObject();
            if (!(raw instanceof Snapshot)) {
                throw new IOException("File is not a marks snapshot: " + filePath);
            }
            return (Snapshot) raw;
        }
    }

    /**
     * Serializable container for a marks backup.
     */
    public static final class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        private final List<Student> students;
        private final List<Subject> subjects;
        private final Map<String, Map<String, Mark>> marksByStudent;

        private Snapshot(List<Student> students, List<Subject> subjects,
                         Map<String, Map<String, Mark>> marksByStudent) {
            this.students = students;
            this.subjects = subjects;
            this.marksByStudent = marksByStudent;
        }

        static Snapshot from(List<Student> students, List<Subject> subjects) {
            List<Student> studentCopy = new ArrayList<>(students);
            List<Subject> subjectCopy = new ArrayList<>(subjects);
            Map<String, Map<String, Mark>> marks = new LinkedHashMap<>();
            for (Student student : students) {
                marks.put(student.getStudentId(), new LinkedHashMap<>(student.getMarks()));
            }
            return new Snapshot(studentCopy, subjectCopy, marks);
        }

        public List<Student> getStudents() {
            return students;
        }

        public List<Subject> getSubjects() {
            return subjects;
        }

        public Map<String, Map<String, Mark>> getMarksByStudent() {
            return marksByStudent;
        }

        /**
         * @return Short summary for the CLI or a dialog.
         */
        public String describe() {
            int markCount = marksByStudent.values().stream().mapToInt(Map::size).sum();
            return "Snapshot students=" + students.size()
                    + ", subjects=" + subjects.size()
                    + ", mark records=" + markCount;
        }
    }
}
