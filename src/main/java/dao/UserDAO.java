package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.Admin;
import model.Teacher;
import model.User;

/** Reads users from the database. Returns an Admin or a Teacher object depending on the role. */
public class UserDAO {

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
