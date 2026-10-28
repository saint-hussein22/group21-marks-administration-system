package com.group21.marksystem.gui;

import com.group21.marksystem.models.Mark;
import com.group21.marksystem.models.Student;
import com.group21.marksystem.models.Subject;
import com.group21.marksystem.models.User;
import com.group21.marksystem.services.MarkService;
import com.group21.marksystem.services.ReportService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Dashboard panel showing system overview and statistics.
 *
 * @author Group 21
 */
public class DashboardPanel extends JPanel {
    private MarkService markService;
    private ReportService reportService;
    private User currentUser;

    public DashboardPanel(MarkService markService, ReportService reportService, User currentUser) {
        this.markService = markService;
        this.reportService = reportService;
        this.currentUser = currentUser;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("Marks Administration System - Dashboard", JLabel.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(headerLabel, BorderLayout.NORTH);

        // Stats panel
        JPanel statsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("System Overview"));

        List<Student> students = markService.getAllStudents();
        List<Subject> subjects = markService.getAllSubjects();

        statsPanel.add(createStatCard("Students", String.valueOf(students.size())));
        statsPanel.add(createStatCard("Subjects", String.valueOf(subjects.size())));

        // Calculate total marks entered
        int totalMarks = 0;
        for (Student s : students) {
            for (Subject subj : subjects) {
                if (s.getMark(subj.getSubjectCode()) != null) {
                    totalMarks++;
                }
            }
        }

        statsPanel.add(createStatCard("Marks Entered", String.valueOf(totalMarks)));
        statsPanel.add(createStatCard("Current User", currentUser.getRole().name()));
        statsPanel.add(createStatCard("Department", currentUser.getDepartment()));

        add(statsPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(10));
        JButton refreshBtn = new JButton("Refresh");
        JButton activityBtn = new JButton("View Activity Log");
        JButton missingBtn = new JButton("View Missing Marks");

        refreshBtn.addActionListener(e -> refresh());
        activityBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    new JScrollPane(new JTextArea(reportService.generateActivityLogReport())),
                    "Activity Log",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        missingBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    new JScrollPane(new JTextArea(reportService.generateMissingMarksReport())),
                    "Missing Marks Report",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        buttonPanel.add(refreshBtn);
        buttonPanel.add(activityBtn);
        buttonPanel.add(missingBtn);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createStatCard(String title, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        JLabel valueLabel = new JLabel(value, JLabel.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 20));
        valueLabel.setForeground(new Color(0, 100, 200));

        card.add(Box.createVerticalGlue());
        card.add(titleLabel);
        card.add(Box.createVerticalGlue());
        card.add(valueLabel);
        card.add(Box.createVerticalGlue());

        return card;
    }

    /**
     * Reloads the dashboard statistics from the shared service.
     */
    public void refreshData() {
        refresh();
    }

    private void refresh() {
        removeAll();
        initComponents();
        revalidate();
        repaint();
    }
}
