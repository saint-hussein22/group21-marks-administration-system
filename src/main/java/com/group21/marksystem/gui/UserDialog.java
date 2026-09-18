package com.group21.marksystem.gui;

import com.group21.marksystem.models.User;
import com.group21.marksystem.services.ReportService;
import com.group21.marksystem.services.UserService;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for adding/editing users.
 *
 * @author Group 21
 */
public class UserDialog extends JDialog {
    private JTextField usernameField, fullNameField, deptField;
    private JPasswordField passwordField;
    private JComboBox<User.Role> roleCombo;
    private UserService userService;
    private ReportService reportService;
    private User user;
    private boolean saved = false;

    public UserDialog(User user, UserService userService, ReportService reportService) {
        super((Frame) null, user == null ? "Add User" : "Edit User", true);
        this.user = user;
        this.userService = userService;
        this.reportService = reportService;
        initComponents();
        if (user != null) {
            populateFields();
        }
    }

    private void initComponents() {
        setSize(400, 300);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        roleCombo = new JComboBox<>(User.Role.values());
        panel.add(roleCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        fullNameField = new JTextField(15);
        panel.add(fullNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Department:"), gbc);
        gbc.gridx = 1;
        deptField = new JTextField(15);
        panel.add(deptField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel();
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        saveBtn.addActionListener(e -> save());
        cancelBtn.addActionListener(e -> dispose());
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel, gbc);

        add(new JScrollPane(panel));
    }

    private void populateFields() {
        usernameField.setText(user.getUsername());
        usernameField.setEditable(false);
        roleCombo.setSelectedItem(user.getRole());
        fullNameField.setText(user.getFullName());
        deptField.setText(user.getDepartment());
    }

    private void save() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        User.Role role = (User.Role) roleCombo.getSelectedItem();
        String fullName = fullNameField.getText().trim();
        String dept = deptField.getText().trim();

        if (username.isEmpty() || fullName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields.");
            return;
        }

        if (user == null) {
            // New user - password required
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Password cannot be empty for new users.");
                return;
            }
            if (userService.getUser(username) != null) {
                JOptionPane.showMessageDialog(this, "Username already exists.");
                return;
            }
            userService.registerUser(username, password, role, fullName, dept);
            reportService.logActivity("Added user: " + username);
        } else {
            // Update existing user
            userService.updateUser(username,
                    password.isEmpty() ? null : password,
                    role, fullName, dept);
            reportService.logActivity("Updated user: " + username);
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }
}
