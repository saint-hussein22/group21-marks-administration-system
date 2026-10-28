package com.group21.marksystem.utils;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Seed data for the CPUT Bellville campus engineering streams.
 *
 * <p>The module codes below follow CPUT's subject-code style and cover the
 * engineering and built-environment programmes offered at the Bellville
 * campus. Lecturers can rename, add or remove any module in the Subjects tab;
 * this data is only used to seed an empty database.</p>
 *
 * @author Group 21
 */
public final class CPUTData {

    public static final String CIVIL = "Diploma in Engineering: Civil Engineering";
    public static final String MECHANICAL = "Diploma in Engineering: Mechanical Engineering";
    public static final String ELECTRICAL = "Diploma in Engineering: Electrical Engineering";
    public static final String ELECTRONIC = "Diploma in Engineering: Electronic & Computer Engineering";
    public static final String CHEMICAL = "Diploma in Engineering: Chemical Engineering";
    public static final String CONSTRUCTION = "Diploma in Construction Management";
    public static final String QUANTITY_SURVEYING = "Diploma in Quantity Surveying";

    /** Example CPUT student number used as the random-number anchor. */
    public static final String FIRST_STUDENT_NUMBER = "253243782";

    private static final String STUDENT_NUMBER_PREFIX = "25";
    private static final long STUDENT_NUMBER_SEED = 253243782L;

    private CPUTData() {
        // Utility class
    }

    /**
     * @return The CPUT Bellville engineering modules.
     */
    public static List<Subject> subjects() {
        List<Subject> subjects = new ArrayList<>();

        // Common first-year engineering modules
        subjects.add(subject("MAT150S", "Engineering Mathematics 1", 12, CIVIL, 1,
                "Test 1", 20, "Test 2", 20, "Assignment", 20, "Exam", 40));
        subjects.add(subject("PHY150S", "Engineering Physics 1", 12, MECHANICAL, 1,
                "Practical", 20, "Test", 30, "Exam", 50));
        subjects.add(subject("CPS150S", "Computer Programming 1", 12, ELECTRONIC, 1,
                "Practical", 40, "Project", 20, "Exam", 40));
        subjects.add(subject("COM150S", "Communication Studies 1", 8, CONSTRUCTION, 1,
                "Assignment", 40, "Exam", 60));

        // Civil Engineering
        subjects.add(subject("CIV150S", "Civil Engineering 1", 12, CIVIL, 1,
                "Drawing", 20, "Practical", 30, "Exam", 50));
        subjects.add(subject("CIV260S", "Structural Analysis 2", 12, CIVIL, 2,
                "Test 1", 25, "Test 2", 25, "Exam", 50));
        subjects.add(subject("CIV270S", "Concrete Technology 2", 12, CIVIL, 2,
                "Practical", 30, "Project", 20, "Exam", 50));
        subjects.add(subject("CIV280S", "Geotechnical Engineering 2", 12, CIVIL, 2,
                "Practical", 40, "Exam", 60));
        subjects.add(subject("CIV370S", "Water Engineering 3", 12, CIVIL, 3,
                "Design Project", 30, "Test", 20, "Exam", 50));
        subjects.add(subject("CIV380S", "Transportation Engineering 3", 12, CIVIL, 3,
                "Project", 30, "Test", 20, "Exam", 50));

        // Mechanical Engineering
        subjects.add(subject("MEC150S", "Mechanical Engineering 1", 12, MECHANICAL, 1,
                "Drawing", 20, "Practical", 30, "Exam", 50));
        subjects.add(subject("MEC260S", "Thermodynamics 2", 12, MECHANICAL, 2,
                "Test 1", 20, "Test 2", 20, "Practical", 10, "Exam", 50));
        subjects.add(subject("MEC270S", "Fluid Mechanics 2", 12, MECHANICAL, 2,
                "Practical", 25, "Test", 25, "Exam", 50));
        subjects.add(subject("MEC280S", "Strength of Materials 2", 12, MECHANICAL, 2,
                "Test 1", 20, "Test 2", 20, "Exam", 60));
        subjects.add(subject("MEC370S", "Machine Design 3", 12, MECHANICAL, 3,
                "Design Project", 40, "Test", 20, "Exam", 40));
        subjects.add(subject("MEC380S", "Manufacturing Technology 3", 12, MECHANICAL, 3,
                "Practical", 40, "Project", 20, "Exam", 40));

        // Electrical, Electronic and Computer Engineering
        subjects.add(subject("EEE150S", "Electrical Engineering 1", 12, ELECTRICAL, 1,
                "Practical", 30, "Test", 20, "Exam", 50));
        subjects.add(subject("EEE260S", "Electrical Machines 2", 12, ELECTRICAL, 2,
                "Practical", 30, "Test", 20, "Exam", 50));
        subjects.add(subject("EEE270S", "Power Systems 2", 12, ELECTRICAL, 2,
                "Test 1", 25, "Test 2", 25, "Exam", 50));
        subjects.add(subject("EEE370S", "Control Systems 3", 12, ELECTRICAL, 3,
                "Practical", 30, "Project", 20, "Exam", 50));
        subjects.add(subject("EEC260S", "Digital Electronics 2", 12, ELECTRONIC, 2,
                "Practical", 40, "Test", 20, "Exam", 40));
        subjects.add(subject("EEC270S", "Microprocessors 2", 12, ELECTRONIC, 2,
                "Practical", 40, "Project", 20, "Exam", 40));
        subjects.add(subject("ECP370S", "Embedded Systems 3", 12, ELECTRONIC, 3,
                "Practical", 40, "Project", 30, "Exam", 30));

        // Chemical Engineering
        subjects.add(subject("CHE150S", "Chemical Engineering 1", 12, CHEMICAL, 1,
                "Practical", 30, "Test", 20, "Exam", 50));
        subjects.add(subject("CHE260S", "Unit Operations 2", 12, CHEMICAL, 2,
                "Practical", 30, "Test", 20, "Exam", 50));
        subjects.add(subject("CHE270S", "Reaction Engineering 2", 12, CHEMICAL, 2,
                "Test 1", 20, "Test 2", 20, "Exam", 60));
        subjects.add(subject("CHE370S", "Process Control 3", 12, CHEMICAL, 3,
                "Practical", 30, "Test", 20, "Exam", 50));
        subjects.add(subject("CHE380S", "Plant Design 3", 12, CHEMICAL, 3,
                "Design Project", 40, "Test", 20, "Exam", 40));

        // Construction Management and Quantity Surveying
        subjects.add(subject("CON150S", "Construction Technology 1", 12, CONSTRUCTION, 1,
                "Drawing", 30, "Test", 20, "Exam", 50));
        subjects.add(subject("CON260S", "Construction Management 2", 12, CONSTRUCTION, 2,
                "Assignment", 30, "Test", 20, "Exam", 50));
        subjects.add(subject("QSV260S", "Quantity Surveying 2", 12, QUANTITY_SURVEYING, 2,
                "Assignment", 30, "Test", 20, "Exam", 50));

        return subjects;
    }

    /**
     * Generates unique, randomised-looking CPUT student numbers in the
     * {@code 25xxxxxxx} intake format. The first number is exactly
     * {@value #FIRST_STUDENT_NUMBER}; the rest are generated from a fixed
     * seed so the demo class list (and the downloaded template) stay stable.
     *
     * @param count Number of student numbers to generate.
     * @return Unique nine-digit student numbers.
     */
    public static List<String> randomStudentNumbers(int count) {
        if (count < 1) {
            return new ArrayList<>();
        }

        Set<String> numbers = new LinkedHashSet<>();
        numbers.add(FIRST_STUDENT_NUMBER);

        Random random = new Random(STUDENT_NUMBER_SEED);
        while (numbers.size() < count) {
            numbers.add(STUDENT_NUMBER_PREFIX + String.format("%07d", random.nextInt(10_000_000)));
        }
        return new ArrayList<>(numbers);
    }

    /**
     * @return A CPUT-style class list with randomised student numbers starting
     *         at 253243782.
     */
    public static List<Student> students() {
        List<Student> students = new ArrayList<>();
        List<String> numbers = randomStudentNumbers(23);
        int index = 0;

        students.add(student(numbers.get(index++), "Hussein", "Akim", CIVIL, 2));
        students.add(student(numbers.get(index++), "Thandiwe", "Moyo", CIVIL, 2));
        students.add(student(numbers.get(index++), "Sipho", "Ndlovu", CIVIL, 2));
        students.add(student(numbers.get(index++), "Ayesha", "Petersen", CIVIL, 3));
        students.add(student(numbers.get(index++), "Liam", "Daniels", CIVIL, 1));

        students.add(student(numbers.get(index++), "Naledi", "Khumalo", MECHANICAL, 2));
        students.add(student(numbers.get(index++), "Ethan", "van Wyk", MECHANICAL, 2));
        students.add(student(numbers.get(index++), "Fatima", "Adams", MECHANICAL, 3));
        students.add(student(numbers.get(index++), "Jason", "Botha", MECHANICAL, 3));
        students.add(student(numbers.get(index++), "Lerato", "Mthembu", MECHANICAL, 1));

        students.add(student(numbers.get(index++), "Kyle", "Fourie", ELECTRICAL, 2));
        students.add(student(numbers.get(index++), "Zanele", "Dlamini", ELECTRICAL, 2));
        students.add(student(numbers.get(index++), "Mohamed", "Ismail", ELECTRICAL, 3));
        students.add(student(numbers.get(index++), "Chloe", "Jacobs", ELECTRICAL, 1));

        students.add(student(numbers.get(index++), "Bongani", "Sithole", ELECTRONIC, 2));
        students.add(student(numbers.get(index++), "Amy", "September", ELECTRONIC, 2));
        students.add(student(numbers.get(index++), "Ruan", "Pretorius", ELECTRONIC, 3));
        students.add(student(numbers.get(index++), "Nomsa", "Mabaso", ELECTRONIC, 1));

        students.add(student(numbers.get(index++), "Tariq", "Hendricks", CHEMICAL, 2));
        students.add(student(numbers.get(index++), "Michelle", "Naidoo", CHEMICAL, 2));
        students.add(student(numbers.get(index++), "Andile", "Zulu", CHEMICAL, 3));

        students.add(student(numbers.get(index++), "Samantha", "October", CONSTRUCTION, 2));
        students.add(student(numbers.get(index++), "Vusi", "Mahlangu", QUANTITY_SURVEYING, 2));

        return students;
    }

    /**
     * Builds realistic sample marks for the seeded class list.
     *
     * @param subjects The subjects that exist in the system.
     * @param students The students that exist in the system.
     * @return Marks ready to be passed to {@code MarkService.enterMarks}.
     */
    public static List<Mark> marks(List<Subject> subjects, List<Student> students) {
        List<Mark> marks = new ArrayList<>();

        for (Student student : students) {
            List<Subject> matches = new ArrayList<>();
            for (Subject subject : subjects) {
                if (subject.getDescription() != null
                        && subject.getDescription().startsWith(student.getProgram())
                        && subject.getDescription().contains("Year " + student.getYear())) {
                    matches.add(subject);
                }
            }
            if (matches.isEmpty()) {
                matches.addAll(subjects);
            }

            int subjectCount = Math.min(2, matches.size());
            for (int s = 0; s < subjectCount; s++) {
                Subject subject = matches.get(s);
                Map<String, Double> assessmentMarks = new LinkedHashMap<>();
                int index = 0;
                for (Map.Entry<String, Double> assessment : subject.getAssessmentWeights().entrySet()) {
                    String key = student.getStudentId() + subject.getSubjectCode() + assessment.getKey();
                    int score = 45 + Math.abs(key.hashCode()) % 51;

                    // Leave the last assessment blank for a few students so the
                    // missing-marks report has something to flag.
                    if (student.getStudentId().endsWith("03") && index == subject.getAssessmentWeights().size() - 1) {
                        assessmentMarks.put(assessment.getKey(), null);
                    } else {
                        assessmentMarks.put(assessment.getKey(), (double) score);
                    }
                    index++;
                }
                marks.add(new Mark(student.getStudentId(), subject.getSubjectCode(), assessmentMarks));
            }
        }

        return marks;
    }

    private static Subject subject(String code, String name, int credits, String programme,
                                   int year, Object... categoryWeights) {
        Subject subject = new Subject(code, name, credits,
                programme + " - Year " + year + " (CPUT Bellville)");
        for (int i = 0; i + 1 < categoryWeights.length; i += 2) {
            subject.addAssessment((String) categoryWeights[i],
                    ((Number) categoryWeights[i + 1]).doubleValue());
        }
        return subject;
    }

    private static Student student(String id, String firstName, String lastName,
                                   String programme, int year) {
        return new Student(id, firstName, lastName, id + "@mycput.ac.za", programme, year);
    }
}
