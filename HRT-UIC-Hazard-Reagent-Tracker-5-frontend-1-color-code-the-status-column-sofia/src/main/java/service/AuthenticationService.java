package service;

import dao.UserDAO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
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
        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null || !user.getPassword().equals(hash(password))) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return user;   // really an Admin or Teacher object
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
