package com.group21.marksystem.gui;

import com.group21.marksystem.models.User;
import com.group21.marksystem.services.ReportService;
import com.group21.marksystem.services.UserService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Panel for managing users (HoD only).
 *
 * @author Group 21
 */
public class UserPanel extends JPanel {
    private UserService userService;
    private ReportService reportService;
    private DefaultTableModel tableModel;
    private JTable table;

    public UserPanel(UserService userService, ReportService reportService) {
        this.userService = userService;
        this.reportService = reportService;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"Username", "Full Name", "Role", "Department"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add User");
        JButton editBtn = new JButton("Edit User");
        JButton deleteBtn = new JButton("Delete User");
        JButton refreshBtn = new JButton("Refresh");

        addBtn.addActionListener(e -> addUser());
        editBtn.addActionListener(e -> editUser());
        deleteBtn.addActionListener(e -> deleteUser());
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
        for (User user : userService.getAllUsers()) {
            tableModel.addRow(new Object[]{
                    user.getUsername(),
                    user.getFullName(),
                    user.getRole(),
                    user.getDepartment()
            });
        }
    }

    private void addUser() {
        UserDialog dialog = new UserDialog(null, userService, reportService);
        dialog.setVisible(true);
        refreshTable();
    }

    private void editUser() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user to edit.");
            return;
        }
        String username = (String) tableModel.getValueAt(selected, 0);
        User user = userService.getUser(username);
        if (user != null) {
            UserDialog dialog = new UserDialog(user, userService, reportService);
            dialog.setVisible(true);
            refreshTable();
        }
    }

    private void deleteUser() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.");
            return;
        }
        String username = (String) tableModel.getValueAt(selected, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this user?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            userService.deleteUser(username);
            refreshTable();
        }
    }
}
