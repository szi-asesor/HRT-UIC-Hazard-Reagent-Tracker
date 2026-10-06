package service;

import dao.ChemicalDAO;
import dao.CompatibilityDAO;
import dao.TransactionDAO;
import java.sql.SQLException;
import java.util.List;
import model.Chemical;
import model.Transaction;

/** Business rules for chemicals: validation, search, compatibility check, dashboard counts. */
public class ChemicalService {
    private final ChemicalDAO chemicalDAO = new ChemicalDAO();
    private final CompatibilityDAO compatibilityDAO = new CompatibilityDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    public List<Chemical> getAll() throws SQLException {
        return chemicalDAO.findAllActive();
    }

    public List<Chemical> search(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAll();
        }
        return chemicalDAO.search(keyword.trim());
    }

    /** isNew = true when adding (quantity must be above zero). */
    public void validate(Chemical c, boolean isNew) {
        if (c.getName() == null || c.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Chemical name is required.");
        }
        if (c.getUnit() == null || c.getUnit().trim().isEmpty()) {
            throw new IllegalArgumentException("Unit is required.");
        }
        if (c.getLocation() == null || c.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Storage location is required.");
        }
        if (c.getStorageGroup() == null || c.getStorageGroup().trim().isEmpty()) {
            throw new IllegalArgumentException("Storage group is required.");
        }
        if (isNew && c.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (c.getMinimumStock() < 0) {
            throw new IllegalArgumentException("Minimum stock cannot be negative.");
        }
    }

    /** Adds the chemical and records an ADD transaction for the starting quantity. */
    public void add(Chemical c, int userId) throws SQLException {
        validate(c, true);
        chemicalDAO.insert(c);
        transactionDAO.insert(new Transaction(c.getId(), userId, "ADD", c.getQuantity()));
    }

    public void update(Chemical c) throws SQLException {
        validate(c, false);
        chemicalDAO.update(c);
    }

    public void deactivate(int chemicalId) throws SQLException {
        chemicalDAO.deactivate(chemicalId);
    }

    /**
     * Compares the chemical's storage group with the groups already in the same location.
     * Returns a warning text, or null when everything is fine.
     */
    public String checkCompatibility(Chemical c) throws SQLException {
        StringBuilder conflicts = new StringBuilder();
        for (Chemical other : chemicalDAO.findByLocation(c.getLocation(), c.getId())) {
            if (compatibilityDAO.areIncompatible(c.getStorageGroup(), other.getStorageGroup())) {
                conflicts.append("  - ").append(other.getName())
                        .append(" (").append(other.getStorageGroup()).append(")\n");
            }
        }
        if (conflicts.length() == 0) {
            return null;
        }
        return "WARNING:\nThese chemicals have incompatible storage groups.\n"
                + "'" + c.getName() + "' (" + c.getStorageGroup() + ") vs:\n" + conflicts;
    }

    // ----- compatibility rules -----

    public List<String[]> getRules() throws SQLException {
        return compatibilityDAO.findAll();
    }

    public void addRule(String groupA, String groupB) throws SQLException {
        if (groupA.equals(groupB)) {
            throw new IllegalArgumentException("Choose two different groups.");
        }
        if (compatibilityDAO.areIncompatible(groupA, groupB)) {
            throw new IllegalArgumentException("That rule already exists.");
        }
        compatibilityDAO.add(groupA, groupB);
    }

    public void updateRule(int ruleId, String groupA, String groupB) throws SQLException {
        if (groupA.equals(groupB)) {
            throw new IllegalArgumentException("Choose two different groups.");
        }
        for (String[] r : compatibilityDAO.findAll()) {
            boolean samePair = (r[1].equals(groupA) && r[2].equals(groupB))
                    || (r[1].equals(groupB) && r[2].equals(groupA));
            if (samePair && !r[0].equals(String.valueOf(ruleId))) {
                throw new IllegalArgumentException("That rule already exists.");
            }
        }
        compatibilityDAO.update(ruleId, groupA, groupB);
    }

    public void deleteRule(int ruleId) throws SQLException {
        compatibilityDAO.delete(ruleId);
    }

    // ----- dashboard counts -----

    public int countLowStock(List<Chemical> list) {
        int n = 0;
        for (Chemical c : list) {
            if (c.isLowStock()) n++;
        }
        return n;
    }

    public int countExpired(List<Chemical> list) {
        int n = 0;
        for (Chemical c : list) {
            if (c.isExpired()) n++;
        }
        return n;
    }
}
