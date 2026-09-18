package com.group21.marksystem.models;

/**
 * Represents a user of the Marks Administration System.
 * Implements role-based access control with two roles: LECTURER and HOD.
 *
 * @author Group 21
 */
public class User {
    private String username;
    private String password; // In production, this would be hashed
    private Role role;
    private String fullName;
    private String department;

    public enum Role {
        LECTURER,
        HOD
    }

    public User(String username, String password, Role role, String fullName, String department) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.department = department;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean hasAccess(Role requiredRole) {
        if (this.role == Role.HOD) {
            return true; // HoD has full access
        }
        return this.role == requiredRole;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s", fullName, role, department);
    }
}
