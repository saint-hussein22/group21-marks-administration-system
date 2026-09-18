package com.group21.marksystem.gui;

import com.group21.marksystem.models.Subject;
import com.group21.marksystem.services.MarkService;
import com.group21.marksystem.services.ReportService;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dialog for adding/editing subjects.
 *
 * @author Group 21
 */
public class SubjectDialog extends JDialog {
    private JTextField codeField, nameField, creditsField, descriptionField;
    private JPanel assessmentsPanel;
    private MarkService markService;
    private ReportService reportService;
    private Subject subject;
    private boolean saved = false;

    public SubjectDialog(Subject subject, MarkService markService, ReportService reportService) {
        super((Frame) null, subject == null ? "Add Subject" : "Edit Subject", true);
        this.subject = subject;
        this.markService = markService;
        this.reportService = reportService;
        initComponents();
        if (subject != null) {
            populateFields();
        }
    }

    private void initComponents() {
        setSize(500, 450);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Subject Code:"), gbc);
        gbc.gridx = 1;
        codeField = new JTextField(15);
        formPanel.add(codeField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Subject Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        formPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Credits:"), gbc);
        gbc.gridx = 1;
        creditsField = new JTextField(15);
        formPanel.add(creditsField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        descriptionField = new JTextField(15);
        formPanel.add(descriptionField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Assessments:"), gbc);

        // Assessments panel
        assessmentsPanel = new JPanel();
        assessmentsPanel.setLayout(new BoxLayout(assessmentsPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(assessmentsPanel);
        scrollPane.setPreferredSize(new Dimension(300, 120));
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(scrollPane, gbc);

        // Add assessment button
        gbc.gridx = 1; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE;
        JButton addAssessmentBtn = new JButton("Add Assessment");
        addAssessmentBtn.addActionListener(e -> addAssessmentField("", ""));
        formPanel.add(addAssessmentBtn, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        saveBtn.addActionListener(e -> save());
        cancelBtn.addActionListener(e -> dispose());
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(panel);

        // Add initial assessment fields
        addAssessmentField("", "");
    }

    private void addAssessmentField(String category, String weight) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JTextField catField = new JTextField(category, 10);
        JTextField wtField = new JTextField(weight, 5);
        JButton delBtn = new JButton("Remove");
        delBtn.addActionListener(e -> {
            assessmentsPanel.remove(row);
            assessmentsPanel.revalidate();
            assessmentsPanel.repaint();
        });
        row.add(new JLabel("Category:"));
        row.add(catField);
        row.add(new JLabel("Weight:"));
        row.add(wtField);
        row.add(delBtn);
        assessmentsPanel.add(row);
        assessmentsPanel.revalidate();
        assessmentsPanel.repaint();
    }

    private void populateFields() {
        codeField.setText(subject.getSubjectCode());
        nameField.setText(subject.getSubjectName());
        creditsField.setText(String.valueOf(subject.getCredits()));
        descriptionField.setText(subject.getDescription());

        for (Map.Entry<String, Double> entry : subject.getAssessmentWeights().entrySet()) {
            addAssessmentField(entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private void save() {
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String desc = descriptionField.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter subject code and name.");
            return;
        }

        int credits;
        try {
            credits = Integer.parseInt(creditsField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid credits number.");
            return;
        }

        // Collect assessments
        Map<String, Double> weights = new LinkedHashMap<>();
        for (Component comp : assessmentsPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel row = (JPanel) comp;
                JTextField catField = (JTextField) row.getComponent(1);
                JTextField wtField = (JTextField) row.getComponent(3);
                String cat = catField.getText().trim();
                String wt = wtField.getText().trim();
                if (!cat.isEmpty() && !wt.isEmpty()) {
                    try {
                        weights.put(cat, Double.parseDouble(wt));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (subject == null) {
            Subject newSubject = new Subject(code, name, credits, desc);
            for (Map.Entry<String, Double> entry : weights.entrySet()) {
                newSubject.addAssessment(entry.getKey(), entry.getValue());
            }
            markService.addSubject(newSubject);
            reportService.logActivity("Added subject: " + code);
        } else {
            // Note: For editing, we would update the existing subject
            reportService.logActivity("Edited subject: " + code);
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }
}
