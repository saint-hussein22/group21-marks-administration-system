package com.group21.marksystem.gui;

import com.group21.marksystem.models.Student;
import com.group21.marksystem.services.MarkService;
import com.group21.marksystem.services.ReportService;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for adding/editing students.
 *
 * @author Group 21
 */
public class StudentDialog extends JDialog {
    private JTextField idField, firstNameField, lastNameField, emailField, programField, yearField;
    private boolean saved = false;
    private MarkService markService;
    private ReportService reportService;
    private Student student;

    public StudentDialog(Student student, MarkService markService, ReportService reportService) {
        super((Frame) null, student == null ? "Add Student" : "Edit Student", true);
        this.student = student;
        this.markService = markService;
        this.reportService = reportService;
        initComponents();
        if (student != null) {
            populateFields();
        }
    }

    private void initComponents() {
        setSize(450, 350);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Fields
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Student ID:"), gbc);
        gbc.gridx = 1;
        idField = new JTextField(15);
        panel.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        firstNameField = new JTextField(15);
        panel.add(firstNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        lastNameField = new JTextField(15);
        panel.add(lastNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(15);
        panel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Program:"), gbc);
        gbc.gridx = 1;
        programField = new JTextField(15);
        panel.add(programField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Year:"), gbc);
        gbc.gridx = 1;
        yearField = new JTextField(15);
        panel.add(yearField, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel();
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> save());
        cancelBtn.addActionListener(e -> {
            saved = false;
            dispose();
        });

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel, gbc);

        add(new JScrollPane(panel));
    }

    private void populateFields() {
        if (student != null) {
            idField.setText(student.getStudentId());
            firstNameField.setText(student.getFirstName());
            lastNameField.setText(student.getLastName());
            emailField.setText(student.getEmail());
            programField.setText(student.getProgram());
            yearField.setText(String.valueOf(student.getYear()));
            idField.setEditable(false);
        }
    }

    private void save() {
        String id = idField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String program = programField.getText().trim();

        if (id.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all required fields (ID, First Name, Last Name).");
            return;
        }

        int year;
        try {
            year = Integer.parseInt(yearField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid year.");
            return;
        }

        try {
            if (student == null) {
                // New student
                if (markService.getStudent(id) != null) {
                    JOptionPane.showMessageDialog(this, "Student ID already exists.");
                    return;
                }
                Student newStudent = new Student(id, firstName, lastName, email, program, year);
                markService.addStudent(newStudent);
                reportService.logActivity("Added student: " + firstName + " " + lastName + " (" + id + ")");
            } else {
                // Update existing
                markService.updateStudent(id, firstName, lastName, email, program, year);
                reportService.logActivity("Updated student: " + firstName + " " + lastName + " (" + id + ")");
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Invalid Student", JOptionPane.ERROR_MESSAGE);
            return;
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }
}
