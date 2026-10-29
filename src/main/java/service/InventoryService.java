package service;

import dao.ChemicalDAO;
import dao.DatabaseConnection;
import dao.TransactionDAO;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import model.Chemical;
import model.Transaction;

/** Increases / decreases stock and records every change as a Transaction. */
public class InventoryService {
    private final ChemicalDAO chemicalDAO = new ChemicalDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    public void addStock(int chemicalId, double amount, int userId) throws SQLException {
        change(chemicalId, amount, userId, "ADD");
    }

    public void releaseStock(int chemicalId, double amount, int userId) throws SQLException {
        change(chemicalId, amount, userId, "RELEASE");
    }

    public List<Transaction> getTransactions() throws SQLException {
        return transactionDAO.findAll();
    }

    private void change(int chemicalId, double amount, int userId, String type) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                Chemical c = chemicalDAO.findById(con, chemicalId);
                if (c == null) {
                    throw new IllegalArgumentException("Chemical not found.");
                }
                if (type.equals("ADD")) {
                    c.addStock(amount);
                    chemicalDAO.updateQuantity(con, c.getId(), c.getQuantity());
                    transactionDAO.insert(con, new Transaction(c.getId(), userId, "ADD", amount));
                } else {
                    release(con, c, amount, userId);
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Puts released stock back (used when an APPROVED request is changed to another status). */
    public void returnStock(Connection con, Chemical c, double amount, int userId) throws SQLException {
        c.addStock(amount);
        chemicalDAO.updateQuantity(con, c.getId(), c.getQuantity());
        transactionDAO.insert(con, new Transaction(c.getId(), userId, "ADJUSTMENT", amount));
    }

    /**
     * Takes stock out of a chemical and records a RELEASE transaction.
     * Also used by RequestService when a request is approved.
     */
    public void release(Connection con, Chemical c, double amount, int userId) throws SQLException {
        if (amount > c.getQuantity()) {
            throw new IllegalArgumentException("Insufficient inventory for " + c.getName() + ".");
        }
        c.release(amount);   // Chemical itself also refuses invalid amounts
        chemicalDAO.updateQuantity(con, c.getId(), c.getQuantity());
        transactionDAO.insert(con, new Transaction(c.getId(), userId, "RELEASE", amount));
    }
}
