package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import model.Chemical;

/** All SQL for the chemicals table. */
public class ChemicalDAO {
    private static final String ACTIVE = "SELECT * FROM chemicals WHERE active = TRUE";

    public List<Chemical> findAllActive() throws SQLException {
        return query(ACTIVE + " ORDER BY name");
    }

    /** Search by chemical name or hazard type. */
    public List<Chemical> search(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        return query(ACTIVE + " AND (name LIKE ? OR hazard_type LIKE ?) ORDER BY name", like, like);
    }

    /** Chemicals stored in the same location (used for the compatibility check). */
    public List<Chemical> findByLocation(String location, int excludeId) throws SQLException {
        return query(ACTIVE + " AND location = ? AND id <> ?", location, excludeId);
    }

    public Chemical findById(int id) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return findById(con, id);
        }
    }

    /** Version that uses an existing connection (needed inside a database transaction). */
    public Chemical findById(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM chemicals WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public void insert(Chemical c) throws SQLException {
        String sql = "INSERT INTO chemicals (name, quantity, unit, hazard_type, storage_group, "
                + "storage_temperature, location, minimum_stock, expiration_date) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setFields(ps, c);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Chemical c) throws SQLException {
        String sql = "UPDATE chemicals SET name=?, unit=?, hazard_type=?, storage_group=?, "
                + "storage_temperature=?, location=?, minimum_stock=?, expiration_date=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getUnit());
            ps.setString(3, c.getHazardType());
            ps.setString(4, c.getStorageGroup());
            ps.setString(5, c.getStorageTemperature());
            ps.setString(6, c.getLocation());
            ps.setDouble(7, c.getMinimumStock());
            if (c.getExpirationDate() == null) {
                ps.setNull(8, java.sql.Types.DATE);
            } else {
                ps.setDate(8, Date.valueOf(c.getExpirationDate()));
            }
            ps.setInt(9, c.getId());
            ps.executeUpdate();
        }
    }

    /** "Remove" only hides the chemical (active = false) so old requests still make sense. */
    public void deactivate(int id) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE chemicals SET active = FALSE WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void updateQuantity(Connection con, int id, double newQuantity) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE chemicals SET quantity = ? WHERE id = ?")) {
            ps.setDouble(1, newQuantity);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // ----- helpers -----

    private List<Chemical> query(String sql, Object... params) throws SQLException {
        List<Chemical> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private void setFields(PreparedStatement ps, Chemical c) throws SQLException {
        ps.setString(1, c.getName());
        ps.setDouble(2, c.getQuantity());
        ps.setString(3, c.getUnit());
        ps.setString(4, c.getHazardType());
        ps.setString(5, c.getStorageGroup());
        ps.setString(6, c.getStorageTemperature());
        ps.setString(7, c.getLocation());
        ps.setDouble(8, c.getMinimumStock());
        if (c.getExpirationDate() == null) {
            ps.setNull(9, java.sql.Types.DATE);
        } else {
            ps.setDate(9, Date.valueOf(c.getExpirationDate()));
        }
    }

    private Chemical mapRow(ResultSet rs) throws SQLException {
        Chemical c = new Chemical();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setQuantity(rs.getDouble("quantity"));
        c.setUnit(rs.getString("unit"));
        c.setHazardType(rs.getString("hazard_type"));
        c.setStorageGroup(rs.getString("storage_group"));
        c.setStorageTemperature(rs.getString("storage_temperature"));
        c.setLocation(rs.getString("location"));
        c.setMinimumStock(rs.getDouble("minimum_stock"));
        Date d = rs.getDate("expiration_date");
        if (d != null) {
            c.setExpirationDate(d.toLocalDate());
        }
        return c;
    }
}
