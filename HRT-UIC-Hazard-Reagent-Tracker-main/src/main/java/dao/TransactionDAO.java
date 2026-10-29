package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Transaction;

/** SQL for the transactions table (the history of inventory changes). */
public class TransactionDAO {

    public void insert(Transaction t) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            insert(con, t);
        }
    }

    public void insert(Connection con, Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions (chemical_id, user_id, type, quantity) VALUES (?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, t.getChemicalId());
            ps.setInt(2, t.getUserId());
            ps.setString(3, t.getType());
            ps.setDouble(4, t.getQuantity());
            ps.executeUpdate();
        }
    }

    public List<Transaction> findAll() throws SQLException {
        String sql = "SELECT t.*, c.name AS chemical_name, u.name AS user_name FROM transactions t "
                + "JOIN chemicals c ON t.chemical_id = c.id JOIN users u ON t.user_id = u.id "
                + "ORDER BY t.id DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Transaction t = new Transaction(rs.getInt("chemical_id"), rs.getInt("user_id"),
                        rs.getString("type"), rs.getDouble("quantity"));
                t.setId(rs.getInt("id"));
                t.setChemicalName(rs.getString("chemical_name"));
                t.setUserName(rs.getString("user_name"));
                t.setDate(rs.getTimestamp("transaction_date").toLocalDateTime());
                list.add(t);
            }
        }
        return list;
    }
}
