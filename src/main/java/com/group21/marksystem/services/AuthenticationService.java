package com.group21.marksystem.services;

import com.group21.marksystem.models.User;

import java.util.Collection;

/**
 * Authentication and account contract used by the CLI and GUI.
 *
 * <p>Login, role checks and user management go through this interface.
 * {@link UserService} is the implementation backed by {@code MarkRepository}.</p>
 *
 * @author Group 21
 */
public interface AuthenticationService {

    /**
     * Authenticates a user and, on success, records that user as current.
     *
     * @param username The username.
     * @param password The plain-text password.
     * @return The authenticated user, or null when the credentials are wrong.
     */
    User authenticate(String username, String password);

    /**
     * Creates an account. The password is hashed before it is stored.
     *
     * @param username   The username.
     * @param password   The plain-text password.
     * @param role       The role.
     * @param fullName   The full name.
     * @param department The department.
     * @return The created user, or null when the username is already taken.
     */
    User registerUser(String username, String password, User.Role role,
                      String fullName, String department);

    /**
     * Looks up one account.
     *
     * @param username The username.
     * @return The user, or null when no such account exists.
     */
    User getUser(String username);

    /**
     * @return Every registered user.
     */
    Collection<User> getAllUsers();

    /**
     * Updates an existing account. A null or empty password leaves the current hash in place.
     *
     * @param username    The username.
     * @param newPassword The new plain-text password, or null/empty to keep the current one.
     * @param role        The new role.
     * @param fullName    The new full name.
     * @param department  The new department.
     * @return true when the account was updated.
     */
    boolean updateUser(String username, String newPassword, User.Role role,
                       String fullName, String department);

    /**
     * Deletes an account.
     *
     * @param username The username.
     * @return true when the account was deleted.
     */
    boolean deleteUser(String username);

    /**
     * @return The user who is logged in, or null.
     */
    User getCurrentUser();

    /**
     * Sets the user who is logged in.
     *
     * @param currentUser The current user, or null to clear the session.
     */
    void setCurrentUser(User currentUser);

    /**
     * Checks whether the logged-in user has the required role.
     *
     * @param requiredRole The role required for an action.
     * @return true when the current user has that access.
     */
    boolean hasRole(User.Role requiredRole);

    /**
     * Creates the demo lecturer and HoD accounts when no users exist yet.
     */
    void initializeDefaultUsers();
}
