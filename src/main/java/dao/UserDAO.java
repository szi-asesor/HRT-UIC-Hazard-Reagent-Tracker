package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Admin;
import model.Teacher;
import model.User;

/** Reads users and creates teacher accounts in the database. */
public class UserDAO {

    /** Returns email addresses beginning with the typed prefix, limited for the login dropdown. */
    public List<String> findEmailSuggestions(String prefix) throws SQLException {
        String sql = "SELECT email FROM users WHERE LOWER(LEFT(email, CHAR_LENGTH(?))) = LOWER(?) "
                + "ORDER BY email LIMIT 8";
        List<String> emails = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prefix);
            ps.setString(2, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) emails.add(rs.getString("email"));
            }
        }
        return emails;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return buildUser(rs);
                }
            }
        }
        return null;
    }

    public void addTeacher(String name, String passwordHash, String email) throws SQLException {
        String sql = "INSERT INTO users (name, password, email, role) VALUES (?, ?, ?, 'TEACHER')";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, passwordHash);
            ps.setString(3, email);
            ps.executeUpdate();
        }
    }

    /** The return type is User, but the object is really an Admin or a Teacher. */
    private User buildUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String password = rs.getString("password");
        String email = rs.getString("email");
        if (rs.getString("role").equals("ADMIN")) {
            return new Admin(id, name, password, email);
        }
        return new Teacher(id, name, password, email);
    }
}
