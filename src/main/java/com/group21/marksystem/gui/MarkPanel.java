package com.group21.marksystem.gui;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.models.User;
import com.group21.marksystem.services.CSVService;
import com.group21.marksystem.services.MarkService;
import com.group21.marksystem.services.ReportService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Panel for managing marks entry and view.
 *
 * @author Group 21
 */
public class MarkPanel extends JPanel {
    private MarkService markService;
    private ReportService reportService;
    private CSVService csvService;
    private User currentUser;

    public MarkPanel(MarkService markService, ReportService reportService, CSVService csvService, User currentUser) {
        this.markService = markService;
        this.reportService = reportService;
        this.csvService = csvService;
        this.currentUser = currentUser;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();

        // Enter Marks tab
        tabbedPane.addTab("Enter Marks", createEnterMarksPanel());
        // View Marks tab
        tabbedPane.addTab("View All Marks", createViewMarksPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createEnterMarksPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Subject selection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Select Subject:"));
        JComboBox<Subject> subjectCombo = new JComboBox<>(markService.getAllSubjects().toArray(new Subject[0]));
        topPanel.add(subjectCombo);

        // Student selection
        topPanel.add(new JLabel("Select Student:"));
        JComboBox<Student> studentCombo = new JComboBox<>(markService.getAllStudents().toArray(new Student[0]));
        topPanel.add(studentCombo);

        panel.add(topPanel, BorderLayout.NORTH);

        // Assessment input fields
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(inputPanel);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Update fields when subject changes
        subjectCombo.addActionListener(e -> {
            inputPanel.removeAll();
            Subject selected = (Subject) subjectCombo.getSelectedItem();
            if (selected != null) {
                for (String category : selected.getAssessmentWeights().keySet()) {
                    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    row.add(new JLabel(category + ":"));
                    JTextField field = new JTextField(10);
                    row.add(field);
                    inputPanel.add(row);
                }
            }
            inputPanel.revalidate();
            inputPanel.repaint();
        });

        // Trigger initial population
        Subject initialSubject = (Subject) subjectCombo.getSelectedItem();
        if (initialSubject != null) {
            for (String category : initialSubject.getAssessmentWeights().keySet()) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
                row.add(new JLabel(category + ":"));
                JTextField field = new JTextField(10);
                row.add(field);
                inputPanel.add(row);
            }
        }

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveBtn = new JButton("Save Marks");
        JButton importCsvBtn = new JButton("Import from CSV");

        saveBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Marks saved successfully!");
            reportService.logActivity("Marks entered via GUI");
        });

        importCsvBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                csvService.importMarksFromCSVAsync(filePath)
                        .thenAccept(count -> JOptionPane.showMessageDialog(this,
                                "Imported " + count + " records from CSV."));
            }
        });

        buttonPanel.add(saveBtn);
        buttonPanel.add(importCsvBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createViewMarksPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        String[] columns = {"Student ID", "Name", "Subject", "Total", "Grade", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Populate table
        for (Student student : markService.getAllStudents()) {
            for (Subject subject : markService.getAllSubjects()) {
                Mark mark = student.getMark(subject.getSubjectCode());
                if (mark != null) {
                    model.addRow(new Object[]{
                            student.getStudentId(),
                            student.getFullName(),
                            subject.getSubjectCode(),
                            String.format("%.2f", mark.isMissing() ? 0.0 : mark.getWeightedTotal()),
                            mark.getLetterGrade(),
                            mark.isMissing() ? "INCOMPLETE" : "OK"
                    });
                }
            }
        }

        return panel;
    }
}
