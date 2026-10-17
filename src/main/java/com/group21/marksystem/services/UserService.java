package com.group21.marksystem.services;

import com.group21.marksystem.models.User;
import com.group21.marksystem.utils.PasswordUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class UserService {
    private Map<String, User> users;
    private ReadWriteLock lock;
    private User currentUser;

    public UserService() {
        this.users = new HashMap<>();
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
        lock.readLock().lock();
        try {
            User user = users.get(username);
            if (user != null && PasswordUtils.verifyPassword(password, user.getPassword())) {
                currentUser = user;
                return user;
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
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
     */
    public User registerUser(String username, String password, User.Role role,
                            String fullName, String department) {
        lock.writeLock().lock();
        try {
            if (users.containsKey(username)) {
                return null; // Username already taken
            }

            String hashedPassword = PasswordUtils.hashPassword(password);
            User user = new User(username, hashedPassword, role, fullName, department);
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
            return users.values();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clears all users from memory.
     */
    public void clearUsers() {
        lock.writeLock().lock();
        try {
            users.clear();
            currentUser = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adds a user that already contains a persisted password hash.
     *
     * @param user user loaded from persistent storage.
     */
    public void addUserFromStorage(User user) {
        lock.writeLock().lock();
        try {
            users.put(user.getUsername(), user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Updates a user's details.
     *
     * @param username   The username of the user to update.
     * @param newPassword The new password (null to keep current).
     * @param role       The new role.
     * @param fullName   The new full name.
     * @param department The new department.
     * @return true if the update was successful.
     */
    public boolean updateUser(String username, String newPassword, User.Role role,
                              String fullName, String department) {
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
            user.setFullName(fullName);
            user.setDepartment(department);
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
            return users.remove(username) != null;
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
        if (currentUser == null) {
            return false;
        }
        return currentUser.hasAccess(requiredRole);
    }

    /**
     * Saves users to a file using object serialization.
     *
     * @param filePath The file path to save to.
     */
    public void saveToFile(String filePath) throws IOException {
        lock.readLock().lock();
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(Paths.get(filePath))))) {
            oos.writeObject(new HashMap<>(users));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Loads users from a serialized file.
     *
     * @param filePath The file path to load from.
     */
    @SuppressWarnings("unchecked")
    public void loadFromFile(String filePath) throws IOException, ClassNotFoundException {
        lock.writeLock().lock();
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(Paths.get(filePath))))) {
            users = (HashMap<String, User>) ois.readObject();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Initializes default users for testing/demo purposes.
     */
    public void initializeDefaultUsers() {
        registerUser("lecturer1", "pass123", User.Role.LECTURER,
                "Dr. John Doe", "Computer Science");
        registerUser("hod1", "admin123", User.Role.HOD,
                "Prof. Jane Smith", "Computer Science");
        registerUser("lecturer2", "pass456", User.Role.LECTURER,
                "Dr. Mike Johnson", "Information Technology");
    }
}
