package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL for the compatibility_rules table (pairs of storage groups that must not be stored together). */
public class CompatibilityDAO {

    public boolean areIncompatible(String groupA, String groupB) throws SQLException {
        String sql = "SELECT COUNT(*) FROM compatibility_rules "
                + "WHERE (group_a = ? AND group_b = ?) OR (group_a = ? AND group_b = ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, groupA);
            ps.setString(2, groupB);
            ps.setString(3, groupB);
            ps.setString(4, groupA);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** Each row is {id, groupA, groupB}. */
    public List<String[]> findAll() throws SQLException {
        List<String[]> rules = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM compatibility_rules ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rules.add(new String[]{rs.getString("id"), rs.getString("group_a"), rs.getString("group_b")});
            }
        }
        return rules;
    }

    public void add(String groupA, String groupB) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO compatibility_rules (group_a, group_b) VALUES (?, ?)")) {
            ps.setString(1, groupA);
            ps.setString(2, groupB);
            ps.executeUpdate();
        }
    }

    public void update(int id, String groupA, String groupB) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE compatibility_rules SET group_a = ?, group_b = ? WHERE id = ?")) {
            ps.setString(1, groupA);
            ps.setString(2, groupB);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM compatibility_rules WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
