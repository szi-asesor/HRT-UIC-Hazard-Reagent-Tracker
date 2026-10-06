package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import model.ChemicalRequest;
import model.RequestItem;

/** SQL for chemical_requests and request_items (one request has many items). */
public class RequestDAO {

    /** Saves the request and all of its items together (all or nothing). */
    public void insert(ChemicalRequest r) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try {
            con.setAutoCommit(false);
            String sql = "INSERT INTO chemical_requests (teacher_id, class_name, experiment) VALUES (?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, r.getTeacherId());
                ps.setString(2, r.getClassName());
                ps.setString(3, r.getExperiment());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        r.setId(keys.getInt(1));
                    }
                }
            }
            String itemSql = "INSERT INTO request_items (request_id, chemical_id, quantity) VALUES (?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(itemSql)) {
                for (RequestItem item : r.getItems()) {
                    ps.setInt(1, r.getId());
                    ps.setInt(2, item.getChemicalId());
                    ps.setDouble(3, item.getQuantity());
                    ps.executeUpdate();
                }
            }
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.close();
        }
    }

    /** Edits a request, but ONLY while it is still PENDING (guarded in the SQL itself). */
    public void update(ChemicalRequest r) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try {
            con.setAutoCommit(false);
            int rows;
            String sql = "UPDATE chemical_requests SET class_name = ?, experiment = ? "
                    + "WHERE id = ? AND status = 'PENDING'";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, r.getClassName());
                ps.setString(2, r.getExperiment());
                ps.setInt(3, r.getId());
                rows = ps.executeUpdate();
            }
            if (rows == 0) {
                throw new IllegalArgumentException("This request is no longer pending, so it cannot be edited.");
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM request_items WHERE request_id = ?")) {
                ps.setInt(1, r.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO request_items (request_id, chemical_id, quantity) VALUES (?,?,?)")) {
                for (RequestItem item : r.getItems()) {
                    ps.setInt(1, r.getId());
                    ps.setInt(2, item.getChemicalId());
                    ps.setDouble(3, item.getQuantity());
                    ps.executeUpdate();
                }
            }
            con.commit();
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.close();
        }
    }

    /** Cancels a request only if it is still PENDING. Returns false if it was not. */
    public boolean cancelIfPending(int id) throws SQLException {
        String sql = "UPDATE chemical_requests SET status = 'CANCELLED' WHERE id = ? AND status = 'PENDING'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<ChemicalRequest> findAll() throws SQLException {
        return query("", null);
    }

    public List<ChemicalRequest> findByTeacher(int teacherId) throws SQLException {
        return query("WHERE r.teacher_id = ?", teacherId);
    }

    public ChemicalRequest findById(int id) throws SQLException {
        List<ChemicalRequest> list = query("WHERE r.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public void updateStatus(int id, String status, String reason) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            updateStatus(con, id, status, reason);
        }
    }

    public void updateStatus(Connection con, int id, String status, String reason) throws SQLException {
        String sql = "UPDATE chemical_requests SET status = ?, rejection_reason = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, reason);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    // ----- helpers -----

    private List<ChemicalRequest> query(String where, Integer param) throws SQLException {
        String sql = "SELECT r.*, u.name AS teacher_name, u.email AS teacher_email "
                + "FROM chemical_requests r JOIN users u ON r.teacher_id = u.id "
                + where + " ORDER BY r.id DESC";
        List<ChemicalRequest> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChemicalRequest r = new ChemicalRequest();
                    r.setId(rs.getInt("id"));
                    r.setTeacherId(rs.getInt("teacher_id"));
                    r.setTeacherName(rs.getString("teacher_name"));
                    r.setTeacherEmail(rs.getString("teacher_email"));
                    r.setClassName(rs.getString("class_name"));
                    r.setExperiment(rs.getString("experiment"));
                    r.setStatus(rs.getString("status"));
                    r.setRejectionReason(rs.getString("rejection_reason"));
                    r.setRequestDate(rs.getTimestamp("request_date").toLocalDateTime());
                    list.add(r);
                }
            }
            for (ChemicalRequest r : list) {
                loadItems(con, r);
            }
        }
        return list;
    }

    private void loadItems(Connection con, ChemicalRequest r) throws SQLException {
        String sql = "SELECT ri.chemical_id, ri.quantity, c.name, c.unit FROM request_items ri "
                + "JOIN chemicals c ON ri.chemical_id = c.id WHERE ri.request_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, r.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    r.addItem(new RequestItem(rs.getInt("chemical_id"), rs.getString("name"),
                            rs.getString("unit"), rs.getDouble("quantity")));
                }
            }
        }
    }
}
