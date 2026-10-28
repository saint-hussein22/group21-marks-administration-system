package com.group21.marksystem.gui;

import com.group21.marksystem.models.User;
import com.group21.marksystem.services.AuthenticationService;
import com.group21.marksystem.services.CSVService;
import com.group21.marksystem.services.MarkService;
import com.group21.marksystem.services.ReportService;
import com.group21.marksystem.services.UserService;

import javax.swing.*;
import java.awt.*;

/**
 * Graphical User Interface for the Marks Administration System.
 * Uses Swing for cross-platform compatibility.
 *
 * @author Group 21
 */
public class GUIMain {
    private MarkService markService;
    private AuthenticationService userService;
    private ReportService reportService;
    private CSVService csvService;
    private JFrame mainFrame;

    public GUIMain() {
        this.markService = new MarkService();
        this.userService = new UserService();
        this.reportService = new ReportService(markService, userService);
        this.csvService = new CSVService(markService);

        // Initialize with sample data
        markService.initializeSampleData();
        userService.initializeDefaultUsers();
    }

    /**
     * Starts the GUI application by showing the login dialog.
     */
    public void start() {
        LoginDialog login = new LoginDialog(userService);
        login.setVisible(true);

        User user = login.getAuthenticatedUser();
        if (user != null) {
            createAndShowGUI(user);
        } else {
            System.exit(0);
        }
    }

    private void createAndShowGUI(User currentUser) {
        mainFrame = new JFrame("Marks Administration System - Group 21");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1000, 700);
        mainFrame.setLocationRelativeTo(null);

        // Create tabbed interface
        JTabbedPane tabbedPane = new JTabbedPane();

        DashboardPanel dashboardPanel = new DashboardPanel(markService, reportService, currentUser);
        StudentPanel studentPanel = new StudentPanel(markService, reportService);
        SubjectPanel subjectPanel = new SubjectPanel(markService, reportService);
        MarkPanel markPanel = new MarkPanel(markService, reportService, csvService, currentUser);
        Runnable refreshAll = () -> {
            dashboardPanel.refreshData();
            studentPanel.refreshData();
            subjectPanel.refreshData();
            markPanel.refreshData();
        };
        markPanel.setOnDataChanged(refreshAll);

        // Add tabs
        tabbedPane.addTab("Dashboard", dashboardPanel);
        tabbedPane.addTab("Students", studentPanel);
        tabbedPane.addTab("Subjects", subjectPanel);
        tabbedPane.addTab("Marks", markPanel);
        tabbedPane.addTab("Reports", new ReportPanel(markService, reportService));
        tabbedPane.addTab("CSV", new CSVPanels(csvService, reportService, refreshAll));

        if (currentUser.getRole() == User.Role.HOD) {
            tabbedPane.addTab("User Management", new UserPanel(userService, reportService));
        }

        // Status bar with user info
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        JLabel statusLabel = new JLabel(String.format(
                "Logged in as: %s (%s) | Department: %s | DB: %s",
                currentUser.getFullName(), currentUser.getRole(), currentUser.getDepartment(),
                markService.getDatabase().getPath()));
        statusPanel.add(statusLabel);

        mainFrame.add(tabbedPane, BorderLayout.CENTER);
        mainFrame.add(statusPanel, BorderLayout.SOUTH);

        mainFrame.setVisible(true);
    }
}
