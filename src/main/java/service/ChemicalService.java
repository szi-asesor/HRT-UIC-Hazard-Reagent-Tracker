package service;

import dao.ChemicalDAO;
import dao.TransactionDAO;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Chemical;
import model.Transaction;

/** Business rules for chemicals: validation, search, compatibility check, dashboard counts. */
public class ChemicalService {
    private static final String[][] INCOMPATIBLE_PAIRS = {
            {"Acid", "Base"}, {"Acid", "Flammable"}, {"Base", "Flammable"},
            {"Oxidizer", "Flammable"}
    };
    private static final String[] ACID_CYANIDE_PAIR = {"Acid", "Cyanide"};
    private static final String[] OXIDIZER_ORGANIC_PAIR = {"Oxidizer", "Organic Material"};
    private final ChemicalDAO chemicalDAO = new ChemicalDAO();
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
        ensureUniqueName(c, 0);
        chemicalDAO.insert(c);
        transactionDAO.insert(new Transaction(c.getId(), userId, "ADD", c.getQuantity()));
    }

    public void update(Chemical c) throws SQLException {
        validate(c, false);
        ensureUniqueName(c, c.getId());
        chemicalDAO.update(c);
    }

    private void ensureUniqueName(Chemical chemical, int excludeId) throws SQLException {
        if (chemicalDAO.hasActiveName(chemical.getName().trim(), excludeId)) {
            throw new IllegalArgumentException("A chemical with this name already exists in active inventory.");
        }
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
        for (Chemical other : chemicalDAO.findAllActive()) {
            boolean sameLocation = canonicalStorageLocation(other.getLocation(), other.getStorageGroup())
                    .equals(canonicalStorageLocation(c.getLocation(), c.getStorageGroup()));
            if (other.getId() != c.getId() && sameLocation
                    && areIncompatibleChemicals(c.getStorageGroup(), c.getHazardType(),
                            other.getStorageGroup(), other.getHazardType())) {
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

    /** Maps legacy A–F cabinet records to the current category names using their stored group. */
    public static String canonicalStorageLocation(String location, String group) {
        if ("Flammable".equals(location) || "Acid Corrosive".equals(location)
                || "Base Corrosive".equals(location) || "Oxidizer".equals(location)
                || "Others".equals(location) || "None".equals(location)) return location;
        if (group == null) return "None";
        return switch (group) {
            case "Acid" -> "Acid Corrosive";
            case "Base" -> "Base Corrosive";
            case "Oxidizer" -> "Oxidizer";
            case "Flammable" -> "Flammable";
            case "Other" -> "Others";
            default -> "None";
        };
    }

    // ----- compatibility rules -----

    public List<String[]> getRules() {
        List<String[]> rules = new ArrayList<>();
        for (String[] pair : INCOMPATIBLE_PAIRS) rules.add(pair.clone());
        rules.add(ACID_CYANIDE_PAIR.clone());
        rules.add(OXIDIZER_ORGANIC_PAIR.clone());
        return rules;
    }

    public static boolean areIncompatibleGroups(String groupA, String groupB) {
        if (groupA == null || groupB == null) return false;
        for (String[] pair : INCOMPATIBLE_PAIRS) {
            if ((pair[0].equalsIgnoreCase(groupA) && pair[1].equalsIgnoreCase(groupB))
                    || (pair[1].equalsIgnoreCase(groupA) && pair[0].equalsIgnoreCase(groupB))) return true;
        }
        return false;
    }

    public static boolean areIncompatibleChemicals(String groupA, String hazardsA,
            String groupB, String hazardsB) {
        if (areIncompatibleGroups(groupA, groupB)) return true;
        return ("Acid".equalsIgnoreCase(groupA) && hasHazard(hazardsB, "Cyanide"))
                || ("Acid".equalsIgnoreCase(groupB) && hasHazard(hazardsA, "Cyanide"))
                || ("Oxidizer".equalsIgnoreCase(groupA) && hasHazard(hazardsB, "Organic Material"))
                || ("Oxidizer".equalsIgnoreCase(groupB) && hasHazard(hazardsA, "Organic Material"));
    }

    private static boolean hasHazard(String hazardTypes, String target) {
        if (hazardTypes == null) return false;
        for (String hazard : hazardTypes.split(",")) {
            if (target.equalsIgnoreCase(hazard.trim())) return true;
        }
        return false;
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
