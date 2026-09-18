package com.group21.marksystem.gui;

import com.group21.marksystem.models.Student;
import com.group21.marksystem.services.MarkService;
import com.group21.marksystem.services.ReportService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Panel for managing students.
 *
 * @author Group 21
 */
public class StudentPanel extends JPanel {
    private MarkService markService;
    private ReportService reportService;
    private JTable table;
    private DefaultTableModel tableModel;

    public StudentPanel(MarkService markService, ReportService reportService) {
        this.markService = markService;
        this.reportService = reportService;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Table
        tableModel = new DefaultTableModel(
                new Object[]{"Student ID", "Full Name", "Email", "Program", "Year"}, 0);
        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton addBtn = new JButton("Add Student");
        JButton editBtn = new JButton("Edit Student");
        JButton deleteBtn = new JButton("Delete Student");
        JButton searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Refresh");

        JTextField searchField = new JTextField(15);
        searchField.setToolTipText("Search by name or ID...");

        addBtn.addActionListener(this::addStudent);
        editBtn.addActionListener(e -> editStudent());
        deleteBtn.addActionListener(e -> deleteStudent());
        searchBtn.addActionListener(e -> searchStudents(searchField.getText()));

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(new JLabel("Search: "));
        buttonPanel.add(searchField);
        buttonPanel.add(searchBtn);
        buttonPanel.add(refreshBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Student student : markService.getAllStudents()) {
            tableModel.addRow(new Object[]{
                    student.getStudentId(),
                    student.getFullName(),
                    student.getEmail(),
                    student.getProgram(),
                    student.getYear()
            });
        }
    }

    private void addStudent(ActionEvent e) {
        StudentDialog dialog = new StudentDialog(null, markService, reportService);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshTable();
        }
    }

    private void editStudent() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Please select a student to edit.");
            return;
        }
        String studentId = (String) tableModel.getValueAt(selected, 0);
        Student student = markService.getStudent(studentId);
        if (student != null) {
            StudentDialog dialog = new StudentDialog(student, markService, reportService);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshTable();
            }
        }
    }

    private void deleteStudent() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Please select a student to delete.");
            return;
        }
        String studentId = (String) tableModel.getValueAt(selected, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this student?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            markService.deleteStudent(studentId);
            refreshTable();
        }
    }

    private void searchStudents(String query) {
        tableModel.setRowCount(0);
        for (Student student : markService.searchStudents(query)) {
            tableModel.addRow(new Object[]{
                    student.getStudentId(),
                    student.getFullName(),
                    student.getEmail(),
                    student.getProgram(),
                    student.getYear()
            });
        }
    }
}
