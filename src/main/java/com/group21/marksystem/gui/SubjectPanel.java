package com.group21.marksystem.gui;

import com.group21.marksystem.models.Subject;
import com.group21.marksystem.services.MarkService;
import com.group21.marksystem.services.ReportService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel for managing subjects.
 *
 * @author Group 21
 */
public class SubjectPanel extends JPanel {
    private MarkService markService;
    private ReportService reportService;
    private DefaultTableModel tableModel;
    private JTable table;

    public SubjectPanel(MarkService markService, ReportService reportService) {
        this.markService = markService;
        this.reportService = reportService;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel(
                new Object[]{"Subject Code", "Subject Name", "Credits", "Assessments"}, 0);
        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Subject");
        JButton editBtn = new JButton("Edit Subject");
        JButton deleteBtn = new JButton("Delete Subject");
        JButton refreshBtn = new JButton("Refresh");

        addBtn.addActionListener(e -> addSubject());
        editBtn.addActionListener(e -> editSubject());
        deleteBtn.addActionListener(e -> deleteSubject());
        refreshBtn.addActionListener(e -> refreshTable());

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(refreshBtn);

        add(buttonPanel, BorderLayout.SOUTH);
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Subject subject : markService.getAllSubjects()) {
            tableModel.addRow(new Object[]{
                    subject.getSubjectCode(),
                    subject.getSubjectName(),
                    subject.getCredits(),
                    String.join(", ", subject.getAssessmentWeights().keySet())
            });
        }
    }

    private void addSubject() {
        SubjectDialog dialog = new SubjectDialog(null, markService, reportService);
        dialog.setVisible(true);
        refreshTable();
    }

    private void editSubject() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Please select a subject to edit.");
            return;
        }
        String code = (String) tableModel.getValueAt(selected, 0);
        Subject subject = markService.getSubject(code);
        if (subject != null) {
            SubjectDialog dialog = new SubjectDialog(subject, markService, reportService);
            dialog.setVisible(true);
            refreshTable();
        }
    }

    private void deleteSubject() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Please select a subject to delete.");
            return;
        }
        String code = (String) tableModel.getValueAt(selected, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this subject?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            // Note: MarkService would need a deleteSubject method
            refreshTable();
        }
    }
}
