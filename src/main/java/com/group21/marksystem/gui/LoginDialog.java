package com.group21.marksystem.gui;

import com.group21.marksystem.models.User;
import com.group21.marksystem.services.AuthenticationService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Login dialog for the Marks Administration System.
 *
 * @author Group 21
 */
public class LoginDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private User authenticatedUser;
    private AuthenticationService userService;

    public LoginDialog(AuthenticationService userService) {
        super((Frame) null, "Login - Marks Administration System", true);
        this.userService = userService;
        this.authenticatedUser = null;
        initComponents();
    }

    private void initComponents() {
        setSize(400, 250);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Marks Administration System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titleLabel, gbc);

        // Instructions
        gbc.gridy = 1;
        panel.add(new JLabel("Enter credentials (default: lecturer1/pass123 or hod1/admin123)"), gbc);

        // Username
        gbc.gridy = 2; gbc.gridwidth = 1;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(15);
        panel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel();
        JButton loginButton = new JButton("Login");
        JButton cancelButton = new JButton("Cancel");

        loginButton.addActionListener(this::performLogin);
        cancelButton.addActionListener(e -> {
            authenticatedUser = null;
            dispose();
        });

        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, gbc);

        add(panel);
    }

    private void performLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        User user = userService.authenticate(username, password);
        if (user != null) {
            authenticatedUser = user;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Invalid username or password.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
        }
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}
