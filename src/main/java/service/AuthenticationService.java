package service;

import dao.UserDAO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Locale;
import model.User;

/** Checks email + password (the email is the login) and returns the matching User (an Admin or a Teacher). */
public class AuthenticationService {
    private final UserDAO userDAO = new UserDAO();

    public User login(String email, String password) throws SQLException {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password cannot be empty.");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        User user = userDAO.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (user == null || !user.getPassword().equals(hash(password))) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return user;   // really an Admin or Teacher object
    }

    public void addTeacher(String name, String email, String password) throws SQLException {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Teacher name cannot be empty.");
        }
        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException("Teacher name cannot exceed 100 characters.");
        }
        if (!normalizedEmail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (normalizedEmail.length() > 100) {
            throw new IllegalArgumentException("Email cannot exceed 100 characters.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        if (userDAO.findByEmail(normalizedEmail) != null) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        try {
            userDAO.addTeacher(normalizedName, hash(password), normalizedEmail);
        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                throw new IllegalArgumentException("An account with this email already exists.", ex);
            }
            throw ex;
        }
    }

    /** Passwords are stored as SHA-256 text, never as plain text. */
    private String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
