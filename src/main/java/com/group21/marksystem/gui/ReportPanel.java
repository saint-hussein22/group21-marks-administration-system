package com.group21.marksystem.gui;

import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.services.MarkService;
import com.group21.marksystem.services.ReportService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel for generating and viewing reports.
 *
 * @author Group 21
 */
public class ReportPanel extends JPanel {
    private MarkService markService;
    private ReportService reportService;

    public ReportPanel(MarkService markService, ReportService reportService) {
        this.markService = markService;
        this.reportService = reportService;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 10, 10));

        JButton studentReportBtn = new JButton("Student Report");
        JButton subjectReportBtn = new JButton("Subject Report");
        JButton missingMarksBtn = new JButton("Missing Marks Report");
        JButton activityLogBtn = new JButton("Activity Log");
        JButton exportCsvBtn = new JButton("Export to CSV");
        JButton gpaReportBtn = new JButton("GPA Summary");

        studentReportBtn.addActionListener(e -> showStudentReport());
        subjectReportBtn.addActionListener(e -> showSubjectReport());
        missingMarksBtn.addActionListener(e -> showMissingMarksReport());
        activityLogBtn.addActionListener(e -> showActivityLog());
        exportCsvBtn.addActionListener(e -> exportToCSV());
        gpaReportBtn.addActionListener(e -> showGPASummary());

        buttonPanel.add(studentReportBtn);
        buttonPanel.add(subjectReportBtn);
        buttonPanel.add(missingMarksBtn);
        buttonPanel.add(activityLogBtn);
        buttonPanel.add(exportCsvBtn);
        buttonPanel.add(gpaReportBtn);

        add(new JLabel("Report Generation", JLabel.CENTER), BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
    }

    private void showStudentReport() {
        String studentId = JOptionPane.showInputDialog(this, "Enter Student ID:");
        if (studentId != null) {
            JTextArea textArea = new JTextArea(reportService.generateStudentReport(studentId.trim()));
            textArea.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(textArea),
                    "Student Report", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showSubjectReport() {
        List<Subject> subjects = markService.getAllSubjects();
        Subject selected = (Subject) JOptionPane.showInputDialog(this,
                "Select Subject:", "Subject Report",
                JOptionPane.QUESTION_MESSAGE, null, subjects.toArray(), null);
        if (selected != null) {
            JTextArea textArea = new JTextArea(reportService.generateSubjectReport(selected.getSubjectCode()));
            textArea.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(textArea),
                    "Subject Report", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showMissingMarksReport() {
        JTextArea textArea = new JTextArea(reportService.generateMissingMarksReport());
        textArea.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea),
                "Missing Marks Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showActivityLog() {
        JTextArea textArea = new JTextArea(reportService.generateActivityLogReport());
        textArea.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea),
                "Activity Log", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Marks CSV File");
        fileChooser.setSelectedFile(new java.io.File("marks_export.csv"));
        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".csv")) {
            filePath += ".csv";
        }

        try {
            reportService.exportMarksToCSV(filePath);
            JOptionPane.showMessageDialog(this,
                    "Exported marks to:\n" + filePath,
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error exporting: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showGPASummary() {
        StringBuilder report = new StringBuilder();
        report.append("GPA Summary Report\n");
        report.append("═══════════════════════════════════════════════════════════\n\n");

        for (Student student : markService.getAllStudents()) {
            double gpa = markService.calculateGPA(student.getStudentId());
            report.append(String.format("%-10s %-25s GPA: %.2f\n",
                    student.getStudentId(), student.getFullName(), gpa));
        }

        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea),
                "GPA Summary", JOptionPane.INFORMATION_MESSAGE);
    }
}
