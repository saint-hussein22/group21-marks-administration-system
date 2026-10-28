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
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Panel for managing marks entry and view.
 *
 * @author Group 21
 */
public class MarkPanel extends JPanel {
    private final MarkService markService;
    private final ReportService reportService;
    private final CSVService csvService;
    private final User currentUser;

    private JComboBox<Subject> subjectCombo;
    private JComboBox<Student> studentCombo;
    private JPanel inputPanel;
    private DefaultTableModel marksTableModel;
    private final Map<String, JTextField> markFields = new LinkedHashMap<>();
    private Runnable onDataChanged = () -> { };

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
        tabbedPane.addTab("Enter Marks", createEnterMarksPanel());
        tabbedPane.addTab("View All Marks", createViewMarksPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createEnterMarksPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Select Subject:"));
        subjectCombo = new JComboBox<>(markService.getAllSubjects().toArray(new Subject[0]));
        topPanel.add(subjectCombo);

        topPanel.add(new JLabel("Select Student:"));
        studentCombo = new JComboBox<>(markService.getAllStudents().toArray(new Student[0]));
        topPanel.add(studentCombo);

        JButton refreshListsBtn = new JButton("Refresh Lists");
        refreshListsBtn.addActionListener(e -> refreshCombos());
        topPanel.add(refreshListsBtn);

        panel.add(topPanel, BorderLayout.NORTH);

        inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        panel.add(new JScrollPane(inputPanel), BorderLayout.CENTER);

        subjectCombo.addActionListener(e -> rebuildMarkFields());
        studentCombo.addActionListener(e -> loadExistingMarks());
        rebuildMarkFields();

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveBtn = new JButton("Save Marks");
        JButton importCsvBtn = new JButton("Import from CSV");
        JButton templateBtn = new JButton("Download Template");

        saveBtn.addActionListener(e -> saveMarks());
        importCsvBtn.addActionListener(e -> importCSV());
        templateBtn.addActionListener(e -> downloadTemplate());

        buttonPanel.add(saveBtn);
        buttonPanel.add(importCsvBtn);
        buttonPanel.add(templateBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void rebuildMarkFields() {
        inputPanel.removeAll();
        markFields.clear();

        Subject selected = (Subject) subjectCombo.getSelectedItem();
        if (selected == null) {
            inputPanel.add(new JLabel("Add a subject before entering marks."));
        } else {
            JLabel heading = new JLabel(String.format("%s - %s", selected.getSubjectCode(),
                    selected.getSubjectName()));
            heading.setFont(heading.getFont().deriveFont(Font.BOLD));
            inputPanel.add(heading);

            for (Map.Entry<String, Double> entry : selected.getAssessmentWeights().entrySet()) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
                row.add(new JLabel(String.format("%s (%.0f%%):", entry.getKey(), entry.getValue())));
                JTextField field = new JTextField(10);
                row.add(field);
                markFields.put(entry.getKey(), field);
                inputPanel.add(row);
            }
            inputPanel.add(new JLabel("Leave a field empty to flag it as missing."));
        }

        inputPanel.revalidate();
        inputPanel.repaint();
        loadExistingMarks();
    }

    /**
     * Shows the selected student's existing assessment marks so saving an edit
     * does not overwrite the fields the lecturer did not touch with blanks.
     */
    private void loadExistingMarks() {
        if (markFields.isEmpty()) {
            return;
        }
        Subject subject = (Subject) subjectCombo.getSelectedItem();
        Student student = (Student) studentCombo.getSelectedItem();
        if (subject == null || student == null) {
            return;
        }

        Mark existing = markService.getMarks(student.getStudentId(), subject.getSubjectCode());
        for (Map.Entry<String, JTextField> entry : markFields.entrySet()) {
            Double value = existing == null ? null
                    : existing.getAssessmentMarks().get(entry.getKey());
            entry.getValue().setText(formatScore(value));
        }
    }

    private static String formatScore(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return "";
        }
        return value == Math.rint(value)
                ? String.valueOf(value.longValue())
                : String.format("%.2f", value);
    }

    private void saveMarks() {
        Subject subject = (Subject) subjectCombo.getSelectedItem();
        Student student = (Student) studentCombo.getSelectedItem();

        if (subject == null || student == null) {
            JOptionPane.showMessageDialog(this,
                    "Select a subject and a student first.",
                    "Nothing to Save", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, Double> assessmentMarks = new LinkedHashMap<>();
        for (Map.Entry<String, JTextField> entry : markFields.entrySet()) {
            String raw = entry.getValue().getText().trim();
            if (raw.isEmpty()) {
                assessmentMarks.put(entry.getKey(), null);
                continue;
            }
            try {
                double value = Double.parseDouble(raw);
                if (!Double.isFinite(value) || value < 0 || value > 100) {
                    JOptionPane.showMessageDialog(this,
                            "Marks must be between 0 and 100 (" + entry.getKey() + ").",
                            "Invalid Mark", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                assessmentMarks.put(entry.getKey(), value);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Invalid number for " + entry.getKey() + ": " + raw,
                        "Invalid Mark", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Mark mark = new Mark(student.getStudentId(), subject.getSubjectCode(), assessmentMarks);
        boolean saved;
        try {
            saved = markService.enterMarks(student.getStudentId(), subject.getSubjectCode(),
                    mark, currentUser.getUsername());
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Invalid Mark", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!saved) {
            JOptionPane.showMessageDialog(this,
                    "The student or subject no longer exists.",
                    "Could Not Save", JOptionPane.ERROR_MESSAGE);
            return;
        }

        reportService.logActivity("Entered marks for " + student.getFullName()
                + " in " + subject.getSubjectCode());
        refreshMarksTable();
        loadExistingMarks();
        onDataChanged.run();

        String message = String.format("Marks saved for %s in %s.%nWeighted total: %.2f%nGrade: %s",
                student.getFullName(), subject.getSubjectCode(),
                mark.getWeightedTotal(), mark.getLetterGrade());
        if (mark.isMissing()) {
            message += "\n\nWarning: one or more assessments are missing.";
        }
        JOptionPane.showMessageDialog(this, message, "Marks Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createViewMarksPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        String[] columns = {"Student ID", "Name", "Subject", "Total", "Grade", "Status"};
        marksTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(marksTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            refreshCombos();
            refreshMarksTable();
        });
        buttonPanel.add(refreshBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        refreshMarksTable();
        return panel;
    }

    private void refreshCombos() {
        Subject selectedSubject = (Subject) subjectCombo.getSelectedItem();
        Student selectedStudent = (Student) studentCombo.getSelectedItem();

        subjectCombo.removeAllItems();
        for (Subject subject : markService.getAllSubjects()) {
            subjectCombo.addItem(subject);
        }
        studentCombo.removeAllItems();
        for (Student student : markService.getAllStudents()) {
            studentCombo.addItem(student);
        }

        if (selectedSubject != null) {
            for (int i = 0; i < subjectCombo.getItemCount(); i++) {
                if (subjectCombo.getItemAt(i).getSubjectCode()
                        .equals(selectedSubject.getSubjectCode())) {
                    subjectCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        if (selectedStudent != null) {
            for (int i = 0; i < studentCombo.getItemCount(); i++) {
                if (studentCombo.getItemAt(i).getStudentId()
                        .equals(selectedStudent.getStudentId())) {
                    studentCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        rebuildMarkFields();
    }

    private void refreshMarksTable() {
        if (marksTableModel == null) {
            return;
        }
        marksTableModel.setRowCount(0);
        for (Student student : markService.getAllStudents()) {
            for (Subject subject : markService.getAllSubjects()) {
                Mark mark = student.getMark(subject.getSubjectCode());
                if (mark != null) {
                    marksTableModel.addRow(new Object[]{
                            student.getStudentId(),
                            student.getFullName(),
                            subject.getSubjectCode(),
                            String.format("%.2f", mark.getWeightedTotal()),
                            mark.getLetterGrade(),
                            mark.isMissing() ? "INCOMPLETE" : "OK"
                    });
                }
            }
        }
    }

    private void importCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Import Marks CSV");
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        csvService.importWithReportAsync(filePath)
                .thenAccept(summary -> SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    JTextArea area = new JTextArea(summary.describe(), 14, 70);
                    area.setEditable(false);
                    area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                    JOptionPane.showMessageDialog(this, new JScrollPane(area),
                            "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                    refreshCombos();
                    refreshMarksTable();
                    onDataChanged.run();
                }))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    SwingUtilities.invokeLater(() -> {
                        setCursor(Cursor.getDefaultCursor());
                        JOptionPane.showMessageDialog(this,
                                "Import failed: " + cause.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                });
    }

    private void downloadTemplate() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Marks Template");
        fileChooser.setSelectedFile(new File("CPUT_Marks_Template.csv"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".csv")) {
            filePath += ".csv";
        }
        try {
            int count = csvService.generateTemplate(filePath);
            JOptionPane.showMessageDialog(this,
                    "Template saved with " + count + " student row(s):\n" + filePath,
                    "Template Downloaded", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Could not create the template: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Registers a callback that refreshes the other tabs after marks change.
     *
     * @param onDataChanged The callback to run.
     */
    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged != null ? onDataChanged : () -> { };
    }

    /**
     * Reloads combos and the marks table from the service.
     */
    public void refreshData() {
        refreshCombos();
        refreshMarksTable();
    }
}
