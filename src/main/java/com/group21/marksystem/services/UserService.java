package com.group21.marksystem.services;

import com.group21.marksystem.db.Database;
import com.group21.marksystem.db.MarkRepository;
import com.group21.marksystem.models.User;
import com.group21.marksystem.utils.PasswordUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service class for managing users and authentication.
 * Uses thread-safe operations with read-write locks.
 *
 * @author Group 21
 */
public class UserService implements AuthenticationService {
    private Map<String, User> users;
    private ReadWriteLock lock;
    private volatile User currentUser;
    private final MarkRepository database;

    /**
     * Creates a service backed by the shared SQLite database.
     */
    public UserService() {
        this(Database.getInstance());
    }

    /**
     * Creates a service backed by the supplied database (used by tests).
     *
     * @param database The database to load from and persist to.
     */
    public UserService(MarkRepository database) {
        this.database = database;
        this.users = new HashMap<>(database.loadUsers());
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Authenticates a user by username and password.
     * If successful, sets the current user.
     *
     * @param username The username.
     * @param password The plain text password.
     * @return The authenticated User, or null if authentication fails.
     */
    public User authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        User user;
        lock.readLock().lock();
        try {
            user = users.get(username);
        } finally {
            lock.readLock().unlock();
        }

        if (user == null || !PasswordUtils.verifyPassword(password, user.getPassword())) {
            return null;
        }

        lock.writeLock().lock();
        try {
            currentUser = user;
        } finally {
            lock.writeLock().unlock();
        }
        return user;
    }

    /**
     * Registers a new user.
     *
     * @param username     The username.
     * @param password     The plain text password (will be hashed).
     * @param role         The user's role.
     * @param fullName     The user's full name.
     * @param department   The user's department.
     * @return The created User, or null if username already exists.
     * @throws IllegalArgumentException If the supplied details are invalid.
     */
    public User registerUser(String username, String password, User.Role role,
                            String fullName, String department) {
        requireText(username, "Username");
        requireText(password, "Password");
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        lock.writeLock().lock();
        try {
            if (users.containsKey(username)) {
                return null; // Username already taken
            }

            String hashedPassword = PasswordUtils.hashPassword(password);
            User user = new User(username, hashedPassword, role,
                    fullName == null ? "" : fullName, department == null ? "" : department);
            database.saveUser(user);
            users.put(username, user);
            return user;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a user by username.
     *
     * @param username The username to look up.
     * @return The User, or null if not found.
     */
    public User getUser(String username) {
        lock.readLock().lock();
        try {
            return users.get(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns all registered users.
     *
     * @return Collection of all users.
     */
    public Collection<User> getAllUsers() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(users.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Updates a user's details.
     *
     * @param username   The username of the user to update.
     * @param newPassword The new password (null or empty to keep current).
     * @param role       The new role.
     * @param fullName   The new full name.
     * @param department The new department.
     * @return true if the update was successful.
     * @throws IllegalArgumentException If the new role is null.
     */
    public boolean updateUser(String username, String newPassword, User.Role role,
                              String fullName, String department) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        lock.writeLock().lock();
        try {
            User user = users.get(username);
            if (user == null) {
                return false;
            }

            if (newPassword != null && !newPassword.isEmpty()) {
                user.setPassword(PasswordUtils.hashPassword(newPassword));
            }
            user.setRole(role);
            user.setFullName(fullName == null ? "" : fullName);
            user.setDepartment(department == null ? "" : department);
            database.saveUser(user);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes a user by username.
     *
     * @param username The username to delete.
     * @return true if the user was deleted.
     */
    public boolean deleteUser(String username) {
        lock.writeLock().lock();
        try {
            if (!users.containsKey(username)) {
                return false;
            }
            database.deleteUser(username);
            users.remove(username);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the currently authenticated user.
     *
     * @return The current user, or null if not authenticated.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Checks if the current user has the required role.
     *
     * @param requiredRole The role required to perform an action.
     * @return true if the current user has the required role.
     */
    public boolean hasRole(User.Role requiredRole) {
        User user = currentUser;
        if (user == null) {
            return false;
        }
        return user.hasAccess(requiredRole);
    }

    /**
     * Initializes default users only when no users exist in the database.
     */
    public void initializeDefaultUsers() {
        lock.writeLock().lock();
        try {
            if (!users.isEmpty()) {
                return;
            }
            registerUser("lecturer1", "pass123", User.Role.LECTURER,
                    "Dr. John Doe", "Civil Engineering");
            registerUser("hod1", "admin123", User.Role.HOD,
                    "Prof. Jane Smith", "Faculty of Engineering");
            registerUser("lecturer2", "pass456", User.Role.LECTURER,
                    "Dr. Mike Johnson", "Mechanical Engineering");
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
    }
}
