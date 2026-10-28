package com.group21.marksystem.cli;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.models.User;
import com.group21.marksystem.services.AuthenticationService;
import com.group21.marksystem.services.CSVService;
import com.group21.marksystem.services.MarkService;
import com.group21.marksystem.services.ReportService;
import com.group21.marksystem.services.UserService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Command-Line Interface for the Marks Administration System.
 *
 * @author Group 21
 */
public class CLIMain {
    private Scanner scanner;
    private MarkService markService;
    private AuthenticationService userService;
    private ReportService reportService;
    private CSVService csvService;

    public CLIMain() {
        this(new Scanner(System.in));
    }

    /**
     * Creates a CLI that reads from the supplied scanner. Sharing the scanner
     * with the launcher keeps piped/scripted input working correctly.
     *
     * @param scanner The input scanner.
     */
    public CLIMain(Scanner scanner) {
        this.scanner = scanner;
        this.markService = new MarkService();
        this.userService = new UserService();
        this.reportService = new ReportService(markService, userService);
        this.csvService = new CSVService(markService);

        // Initialize with sample data
        markService.initializeSampleData();
        userService.initializeDefaultUsers();
    }

    /**
     * Starts the CLI application.
     */
    public void start() {
        System.out.println("Welcome to the Marks Administration System (CLI Mode)\n");

        // Authentication
        User currentUser = authenticate();
        if (currentUser == null) {
            System.out.println("Authentication failed. Exiting.");
            return;
        }

        System.out.println("\n✓ Logged in as: " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        System.out.println("  Department: " + currentUser.getDepartment());

        // Main menu loop
        boolean running = true;
        while (running) {
            displayMainMenu(currentUser.getRole());
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    manageStudents(currentUser);
                    break;
                case "2":
                    manageSubjects(currentUser);
                    break;
                case "3":
                    manageMarks(currentUser);
                    break;
                case "4":
                    generateReports(currentUser);
                    break;
                case "5":
                    csvOperations(currentUser);
                    break;
                case "6":
                    if (currentUser.getRole() == User.Role.HOD) {
                        manageUsers(currentUser);
                    } else {
                        System.out.println("\nThis feature is available to HoD only.\n");
                    }
                    break;
                case "0":
                    running = false;
                    System.out.println("\nAll changes are saved in the SQLite database: "
                            + markService.getDatabase().getPath());
                    System.out.println("Thank you for using the Marks Administration System!");
                    break;
                default:
                    System.out.println("\nInvalid choice. Please try again.\n");
                    break;
            }
        }
    }

    private User authenticate() {
        while (true) {
            System.out.println("═══ Authentication Required ═══");
            System.out.println("Default accounts:");
            System.out.println("  Lecturer: username=lecturer1, password=pass123");
            System.out.println("  HoD:      username=hod1, password=admin123");
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            User user = userService.authenticate(username, password);
            if (user != null) {
                return user;
            }

            System.out.println("\n✗ Authentication failed. Try again? (y/n): ");
            String retry = scanner.nextLine().trim();
            if (retry.equalsIgnoreCase("n")) {
                return null;
            }
            System.out.println();
        }
    }

    private void displayMainMenu(User.Role role) {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    MAIN MENU                             ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  1. Student Management                                    ║");
        System.out.println("║  2. Subject Management                                     ║");
        System.out.println("║  3. Mark Management                                        ║");
        System.out.println("║  4. Reports & Analytics                                    ║");
        System.out.println("║  5. CSV Import/Export                                      ║");
        if (role == User.Role.HOD) {
            System.out.println("║  6. User Management (HoD only)                             ║");
        }
        System.out.println("║                                                            ║");
        System.out.println("║  0. Exit                                                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.print("Enter choice: ");
    }

    // ===== Student Management =====

    private void manageStudents(User currentUser) {
        while (true) {
            System.out.println("\n── Student Management ──");
            System.out.println("  1. Add Student");
            System.out.println("  2. List All Students");
            System.out.println("  3. Search Student");
            System.out.println("  4. Update Student");
            System.out.println("  5. Delete Student");
            System.out.println("  0. Back to Main Menu");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": addStudent(); break;
                case "2": listStudents(); break;
                case "3": searchStudent(); break;
                case "4": updateStudent(); break;
                case "5": deleteStudent(); break;
                case "0": return;
                default: System.out.println("Invalid choice."); break;
            }
        }
    }

    private void addStudent() {
        System.out.println("\n── Add New Student ──");
        String id = readRequired("Student ID: ");

        if (markService.getStudent(id) != null) {
            System.out.println("✗ Student ID already exists.");
            return;
        }

        String firstName = readRequired("First Name: ");
        String lastName = readRequired("Last Name: ");
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Program: ");
        String program = scanner.nextLine().trim();
        int year = readInt("Year (1-10): ", 1, 10);

        Student student = new Student(id, firstName, lastName, email, program, year);
        try {
            if (!markService.addStudent(student)) {
                System.out.println("\n✗ Student ID already exists.\n");
                return;
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\n✗ " + e.getMessage() + "\n");
            return;
        }
        System.out.println("\n✓ Student added successfully.\n");
        reportService.logActivity("Added student: " + student.getFullName() + " (" + id + ")");
    }

    private void listStudents() {
        System.out.println("\n── All Students ──\n");
        System.out.printf("%-10s %-25s %-30s %-15s %s\n", "ID", "Name", "Email", "Program", "Year");
        System.out.println("─────────────────────────────────────────────────────────────────────────");

        for (Student student : markService.getAllStudents()) {
            System.out.printf("%-10s %-25s %-30s %-15s %d\n",
                    student.getStudentId(), student.getFullName(),
                    student.getEmail(), student.getProgram(), student.getYear());
        }
    }

    private void searchStudent() {
        System.out.print("\nEnter search term (name, ID, or email): ");
        String query = scanner.nextLine().trim();
        var results = markService.searchStudents(query);

        if (results.isEmpty()) {
            System.out.println("\nNo students found.\n");
        } else {
            System.out.println("\n── Search Results ──\n");
            for (Student student : results) {
                System.out.println(student);
            }
        }
    }

    private void updateStudent() {
        System.out.print("\nEnter student ID to update: ");
        String id = scanner.nextLine().trim();
        Student student = markService.getStudent(id);

        if (student == null) {
            System.out.println("✗ Student not found.");
            return;
        }

        System.out.println("\nCurrent details:");
        System.out.println("  First Name: " + student.getFirstName());
        System.out.println("  Last Name: " + student.getLastName());
        System.out.println("  Email: " + student.getEmail());
        System.out.println("  Program: " + student.getProgram());
        System.out.println("  Year: " + student.getYear());

        System.out.println("\nEnter new values (press Enter to keep current):");

        System.out.print("First Name [" + student.getFirstName() + "]: ");
        String firstName = scanner.nextLine().trim();
        if (firstName.isEmpty()) firstName = student.getFirstName();

        System.out.print("Last Name [" + student.getLastName() + "]: ");
        String lastName = scanner.nextLine().trim();
        if (lastName.isEmpty()) lastName = student.getLastName();

        System.out.print("Email [" + student.getEmail() + "]: ");
        String email = scanner.nextLine().trim();
        if (email.isEmpty()) email = student.getEmail();

        System.out.print("Program [" + student.getProgram() + "]: ");
        String program = scanner.nextLine().trim();
        if (program.isEmpty()) program = student.getProgram();

        System.out.print("Year [" + student.getYear() + "]: ");
        String yearStr = scanner.nextLine().trim();
        int year = student.getYear();
        if (!yearStr.isEmpty()) {
            try {
                year = Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                System.out.println("  ✗ Invalid year, keeping " + student.getYear());
            }
        }

        try {
            markService.updateStudent(id, firstName, lastName, email, program, year);
        } catch (IllegalArgumentException e) {
            System.out.println("\n✗ " + e.getMessage() + "\n");
            return;
        }
        System.out.println("\n✓ Student updated successfully.\n");
        reportService.logActivity("Updated student: " + student.getFullName() + " (" + id + ")");
    }

    private void deleteStudent() {
        System.out.print("\nEnter student ID to delete: ");
        String id = scanner.nextLine().trim();

        System.out.print("Are you sure? (y/n): ");
        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("y")) {
            if (markService.deleteStudent(id)) {
                System.out.println("\n✓ Student deleted successfully.\n");
                reportService.logActivity("Deleted student ID: " + id);
            } else {
                System.out.println("\n✗ Student not found.\n");
            }
        }
    }

    // ===== Subject Management =====

    private void manageSubjects(User currentUser) {
        while (true) {
            System.out.println("\n── Subject Management ──");
            System.out.println("  1. Add Subject");
            System.out.println("  2. List All Subjects");
            System.out.println("  3. View Subject Details");
            System.out.println("  0. Back to Main Menu");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": addSubject(); break;
                case "2": listSubjects(); break;
                case "3": viewSubjectDetails(); break;
                case "0": return;
                default: System.out.println("Invalid choice."); break;
            }
        }
    }

    private void addSubject() {
        System.out.println("\n── Add New Subject ──");
        String code = readRequired("Subject Code: ");

        if (markService.getSubject(code) != null) {
            System.out.println("✗ Subject code already exists.");
            return;
        }

        String name = readRequired("Subject Name: ");
        int credits = readInt("Credits (1-99): ", 1, 99);
        System.out.print("Description: ");
        String description = scanner.nextLine().trim();

        Subject subject = new Subject(code, name, credits, description);

        System.out.println("\nEnter assessment categories (Enter empty name to finish):");
        while (true) {
            System.out.print("  Category name: ");
            String category = scanner.nextLine().trim();
            if (category.isEmpty()) break;

            if (subject.getAssessmentWeights().containsKey(category)) {
                System.out.println("  ✗ Category already added.");
                continue;
            }
            double weight = readDecimal("  Weight (%): ", 0, 100, false);
            subject.addAssessment(category, weight);
        }

        if (subject.getAssessmentWeights().isEmpty()) {
            System.out.println("\n✗ Add at least one assessment category.\n");
            return;
        }

        double total = subject.getAssessmentWeights().values().stream()
                .mapToDouble(Double::doubleValue).sum();
        if (Math.abs(total - 100.0) > 0.01) {
            System.out.printf("\n✗ Assessment weights must add up to 100%% (currently %.2f%%).\n\n", total);
            return;
        }

        try {
            markService.addSubject(subject);
        } catch (IllegalArgumentException e) {
            System.out.println("\n✗ " + e.getMessage() + "\n");
            return;
        }
        System.out.println("\n✓ Subject added successfully.\n");
        reportService.logActivity("Added subject: " + subject.getSubjectName() + " (" + code + ")");
    }

    private void listSubjects() {
        System.out.println("\n── All Subjects ──\n");
        System.out.printf("%-12s %-35s %-10s %s\n", "Code", "Name", "Credits", "Assessments");
        System.out.println("══════════════════════════════════════════════════════════");

        for (Subject subject : markService.getAllSubjects()) {
            System.out.printf("%-12s %-35s %-10d %s\n",
                    subject.getSubjectCode(), subject.getSubjectName(),
                    subject.getCredits(), subject.getAssessmentWeights().keySet());
        }
    }

    private void viewSubjectDetails() {
        System.out.print("\nEnter subject code: ");
        String code = scanner.nextLine().trim();
        Subject subject = markService.getSubject(code);

        if (subject == null) {
            System.out.println("✗ Subject not found.");
            return;
        }

        System.out.println("\n── Subject Details ──\n");
        System.out.println("Code: " + subject.getSubjectCode());
        System.out.println("Name: " + subject.getSubjectName());
        System.out.println("Credits: " + subject.getCredits());
        System.out.println("Description: " + subject.getDescription());
        System.out.println("\nAssessment Categories:");
        for (Map.Entry<String, Double> entry : subject.getAssessmentWeights().entrySet()) {
            System.out.printf("  %-20s %.1f%%\n", entry.getKey(), entry.getValue());
        }
    }

    // ===== Mark Management =====

    private void manageMarks(User currentUser) {
        while (true) {
            System.out.println("\n── Mark Management ──");
            System.out.println("  1. Enter Marks for Student");
            System.out.println("  2. View Student Marks");
            System.out.println("  3. Generate Student Report");
            System.out.println("  4. Generate Subject Report");
            System.out.println("  5. Missing Marks Report");
            System.out.println("  0. Back to Main Menu");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": enterMarks(currentUser); break;
                case "2": viewStudentMarks(); break;
                case "3": generateStudentReport(); break;
                case "4": generateSubjectReport(); break;
                case "5": generateMissingMarksReport(); break;
                case "0": return;
                default: System.out.println("Invalid choice."); break;
            }
        }
    }

    private void enterMarks(User currentUser) {
        System.out.print("\nEnter student ID: ");
        String studentId = scanner.nextLine().trim();
        Student student = markService.getStudent(studentId);

        if (student == null) {
            System.out.println("✗ Student not found.");
            return;
        }

        System.out.print("Enter subject code: ");
        String subjectCode = scanner.nextLine().trim();
        Subject subject = markService.getSubject(subjectCode);

        if (subject == null) {
            System.out.println("✗ Subject not found.");
            return;
        }

        System.out.println("\n── Enter Marks for " + student.getFullName() + " - " + subject.getSubjectName() + " ──\n");

        Map<String, Double> assessmentMarks = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : subject.getAssessmentWeights().entrySet()) {
            Double score = readDecimal(
                    String.format("  %s (Weight: %.0f%%, 0-100, blank = missing): ",
                            entry.getKey(), entry.getValue()),
                    0, 100, true);
            assessmentMarks.put(entry.getKey(), score);
            if (score == null) {
                System.out.println("  (Left empty - will be flagged as MISSING)");
            }
        }

        Mark mark = new Mark(studentId, subjectCode, assessmentMarks);
        try {
            markService.enterMarks(studentId, subjectCode, mark, currentUser.getUsername());
        } catch (IllegalArgumentException e) {
            System.out.println("\n✗ " + e.getMessage() + "\n");
            return;
        }

        System.out.println("\n✓ Marks entered successfully.");
        System.out.printf("  Weighted Total: %.2f\n", mark.getWeightedTotal());
        System.out.println("  Grade: " + mark.getLetterGrade());
        if (mark.isMissing()) {
            System.out.println("  ⚠  Warning: Missing marks detected!");
        }
        System.out.println();

        reportService.logActivity("Entered marks for " + student.getFullName() + " in " + subject.getSubjectName());
    }

    private void viewStudentMarks() {
        System.out.print("\nEnter student ID: ");
        String studentId = scanner.nextLine().trim();
        Student student = markService.getStudent(studentId);

        if (student == null) {
            System.out.println("✗ Student not found.");
            return;
        }

        System.out.println("\n── Marks for " + student.getFullName() + " ──\n");

        if (student.getMarks().isEmpty()) {
            System.out.println("No marks recorded.\n");
            return;
        }

        for (Map.Entry<String, Mark> entry : student.getMarks().entrySet()) {
            Subject subject = markService.getSubject(entry.getKey());
            Mark mark = entry.getValue();
            if (subject != null && mark != null) {
                System.out.println(subject.getSubjectName() + " (" + entry.getKey() + "):");
                System.out.printf("  Weighted Total: %.2f\n", mark.isMissing() ? 0.0 : mark.getWeightedTotal());
                System.out.println("  Grade: " + mark.getLetterGrade());
                if (mark.isMissing()) {
                    System.out.println("  ⚠  Contains missing marks!");
                }
                System.out.println();
            }
        }
        System.out.println("Cumulative GPA: " + String.format("%.2f", markService.calculateGPA(studentId)));
        System.out.println();
    }

    private void generateStudentReport() {
        System.out.print("\nEnter student ID: ");
        String studentId = scanner.nextLine().trim();
        System.out.println(reportService.generateStudentReport(studentId));
    }

    private void generateSubjectReport() {
        System.out.print("\nEnter subject code: ");
        String subjectCode = scanner.nextLine().trim();
        System.out.println(reportService.generateSubjectReport(subjectCode));
    }

    private void generateMissingMarksReport() {
        System.out.println(reportService.generateMissingMarksReport());
    }

    // ===== CSV Operations =====

    private void csvOperations(User currentUser) {
        while (true) {
            System.out.println("\n── CSV Import/Export ──");
            System.out.println("  1. Import Marks from CSV");
            System.out.println("  2. Export All Marks to CSV");
            System.out.println("  3. Download Marks CSV Template");
            System.out.println("  0. Back to Main Menu");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": importCSV(); break;
                case "2": exportCSV(); break;
                case "3": downloadTemplate(); break;
                case "0": return;
                default: System.out.println("Invalid choice."); break;
            }
        }
    }

    private void importCSV() {
        System.out.print("\nEnter CSV file path: ");
        String filePath = scanner.nextLine().trim();

        System.out.println("\nImporting (this may take a moment)...");
        CompletableFuture<CSVService.ImportSummary> future =
                csvService.importWithReportAsync(filePath);

        try {
            CSVService.ImportSummary summary = future.get();
            System.out.println("\n✓ Import complete.\n");
            System.out.println(summary.describe());
            System.out.println();
            reportService.logActivity("Imported " + summary.getMarksSaved()
                    + " mark record(s) from " + filePath);
        } catch (InterruptedException | ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.out.println("\n✗ Import failed: " + cause.getMessage() + "\n");
        }
    }

    private void exportCSV() {
        System.out.print("\nEnter output file path (e.g., marks_export.csv): ");
        String filePath = scanner.nextLine().trim();

        try {
            int count = csvService.exportMarksToCSV(filePath);
            System.out.println("\n✓ Successfully exported " + count + " record(s) to " + filePath + "\n");
            reportService.logActivity("Exported " + count + " records to " + filePath);
        } catch (IOException e) {
            System.out.println("\n✗ Export failed: " + e.getMessage() + "\n");
        }
    }

    private void downloadTemplate() {
        System.out.print("\nEnter output file path [CPUT_Marks_Template.csv]: ");
        String filePath = scanner.nextLine().trim();
        if (filePath.isEmpty()) {
            filePath = "CPUT_Marks_Template.csv";
        }

        try {
            int count = csvService.generateTemplate(filePath);
            System.out.println("\n✓ Marks template downloaded with " + count
                    + " student row(s) at " + filePath);
            System.out.println("  Fill in the Assessment columns, then use option 1 to import it.\n");
            reportService.logActivity("Downloaded CSV template: " + filePath);
        } catch (IOException e) {
            System.out.println("\n✗ Failed to generate template: " + e.getMessage() + "\n");
        }
    }

    // ===== User Management (HoD only) =====

    /**
     * Reports & analytics submenu.
     *
     * @param currentUser The logged-in user.
     */
    private void generateReports(User currentUser) {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║           REPORTS & ANALYTICS                ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println("  1. Student Report");
        System.out.println("  2. Subject Report");
        System.out.println("  3. Missing Marks Report");
        System.out.println("  4. Activity Log");
        System.out.println("  0. Back to Main Menu");
        System.out.print("\nEnter your choice: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                System.out.print("\nEnter student ID: ");
                String studentId = scanner.nextLine().trim();
                System.out.println(reportService.generateStudentReport(studentId));
                reportService.logActivity(currentUser.getUsername() + " generated student report for: " + studentId);
                break;
            case "2":
                System.out.print("\nEnter subject code: ");
                String subjectCode = scanner.nextLine().trim();
                System.out.println(reportService.generateSubjectReport(subjectCode));
                reportService.logActivity(currentUser.getUsername() + " generated subject report for: " + subjectCode);
                break;
            case "3":
                System.out.println(reportService.generateMissingMarksReport());
                reportService.logActivity(currentUser.getUsername() + " generated missing marks report");
                break;
            case "4":
                System.out.println(reportService.generateActivityLogReport());
                break;
            case "0":
                break;
            default:
                System.out.println("\nInvalid choice. Please try again.");
        }
    }

    private void manageUsers(User currentUser) {
        while (true) {
            System.out.println("\n── User Management (HoD) ──");
            System.out.println("  1. List All Users");
            System.out.println("  2. Add User");
            System.out.println("  3. Update User");
            System.out.println("  4. Delete User");
            System.out.println("  0. Back to Main Menu");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": listUsers(); break;
                case "2": addUser(); break;
                case "3": updateUser(); break;
                case "4": deleteUser(currentUser); break;
                case "0": return;
                default: System.out.println("Invalid choice."); break;
            }
        }
    }

    private void listUsers() {
        System.out.println("\n── All Users ──\n");
        System.out.printf("%-15s %-25s %-12s %-25s %s\n", "Username", "Full Name", "Role", "Department", "Actions");
        System.out.println("══════════════════════════════════════════════════════════");

        for (User user : userService.getAllUsers()) {
            System.out.printf("%-15s %-25s %-12s %-25s %s\n",
                    user.getUsername(), user.getFullName(), user.getRole(),
                    user.getDepartment(), "[Update/Delete]");
        }
        System.out.println();
    }

    private void addUser() {
        System.out.println("\n── Add User ──");
        String username = readRequired("Username: ");

        if (userService.getUser(username) != null) {
            System.out.println("✗ Username already exists.");
            return;
        }

        String password = readRequired("Password: ");
        System.out.println("Role: 1=Lecturer, 2=HoD");
        int roleChoice = readInt("Enter role (1/2): ", 1, 2);
        User.Role role = roleChoice == 2 ? User.Role.HOD : User.Role.LECTURER;

        String fullName = readRequired("Full Name: ");
        System.out.print("Department: ");
        String department = scanner.nextLine().trim();

        try {
            userService.registerUser(username, password, role, fullName, department);
        } catch (IllegalArgumentException e) {
            System.out.println("\n✗ " + e.getMessage() + "\n");
            return;
        }
        System.out.println("\n✓ User added successfully.\n");
        reportService.logActivity("Added user: " + username);
    }

    private void updateUser() {
        System.out.print("\nEnter username to update: ");
        String username = scanner.nextLine().trim();
        User user = userService.getUser(username);

        if (user == null) {
            System.out.println("✗ User not found.");
            return;
        }

        System.out.println("\nCurrent details:");
        System.out.println("  Full Name: " + user.getFullName());
        System.out.println("  Role: " + user.getRole());
        System.out.println("  Department: " + user.getDepartment());

        System.out.print("\nNew password (leave empty to keep current): ");
        String password = scanner.nextLine().trim();
        System.out.println("Role: 1=Lecturer, 2=HoD");
        System.out.print("Enter new role (1/2, Enter to keep): ");
        String roleChoice = scanner.nextLine().trim();
        User.Role role = user.getRole();
        if (!roleChoice.isEmpty()) {
            if ("1".equals(roleChoice)) {
                role = User.Role.LECTURER;
            } else if ("2".equals(roleChoice)) {
                role = User.Role.HOD;
            } else {
                System.out.println("  ✗ Invalid role, keeping " + user.getRole());
            }
        }

        System.out.print("New full name (Enter to keep): ");
        String fullName = scanner.nextLine().trim();
        if (!fullName.isEmpty()) user.setFullName(fullName);

        System.out.print("New department (Enter to keep): ");
        String department = scanner.nextLine().trim();
        if (!department.isEmpty()) user.setDepartment(department);

        try {
            userService.updateUser(username, password.isEmpty() ? null : password, role,
                    user.getFullName(), user.getDepartment());
        } catch (IllegalArgumentException e) {
            System.out.println("\n✗ " + e.getMessage() + "\n");
            return;
        }
        System.out.println("\n✓ User updated successfully.\n");
        reportService.logActivity("Updated user: " + username);
    }

    private void deleteUser(User currentUser) {
        System.out.print("\nEnter username to delete: ");
        String username = scanner.nextLine().trim();

        if (currentUser != null && username.equals(currentUser.getUsername())) {
            System.out.println("\n✗ You cannot delete the account you are logged in with.\n");
            return;
        }

        System.out.print("Are you sure? (y/n): ");
        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("y")) {
            if (userService.deleteUser(username)) {
                System.out.println("\n✓ User deleted successfully.\n");
                reportService.logActivity("Deleted user: " + username);
            } else {
                System.out.println("\n✗ User not found.\n");
            }
        }
    }

    // ===== Input helpers =====

    /**
     * Reads a non-empty line, re-prompting until the user enters something.
     */
    private String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("  ✗ This field cannot be empty.");
        }
    }

    /**
     * Reads a whole number inside an inclusive range.
     */
    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("  ✗ Enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  ✗ Please enter a whole number.");
            }
        }
    }

    /**
     * Reads a decimal inside a range. When {@code allowBlank} is true an empty
     * line returns {@code null}.
     */
    private Double readDecimal(String prompt, double min, double max, boolean allowBlank) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty() && allowBlank) {
                return null;
            }
            try {
                double value = Double.parseDouble(input);
                if (Double.isFinite(value) && value >= min && value <= max) {
                    return value;
                }
                System.out.println("  ✗ Enter a number between " + (int) min + " and " + (int) max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  ✗ Please enter a number.");
            }
        }
    }

}
