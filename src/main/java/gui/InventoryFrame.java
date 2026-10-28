package gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Admin;
import model.Chemical;
import model.Transaction;
import service.ChemicalService;
import service.InventoryService;
import util.Format;

/** Admin screen: add stock, release stock, and see the transaction history. */
public class InventoryFrame extends JFrame {
    private final Admin admin;
    private final ChemicalService chemicalService = new ChemicalService();
    private final InventoryService inventoryService = new InventoryService();

    private final JComboBox<Chemical> chemicalBox = new JComboBox<>();
    private final JTextField amountField = new JTextField(8);
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Date", "Chemical", "Type", "Quantity", "By"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public InventoryFrame(Admin admin) {
        this.admin = admin;
        setTitle("Inventory");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Enable table sorting and prevent column reordering
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        // Auto-refresh when window regains focus
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                refresh();
            }
        });

        JButton addBtn = new JButton("Add Stock");
        JButton releaseBtn = new JButton("Release Stock");
        addBtn.addActionListener(e -> change(true));
        releaseBtn.addActionListener(e -> change(false));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(chemicalBox);
        top.add(new JLabel("Amount"));
        top.add(amountField);
        top.add(addBtn);
        top.add(releaseBtn);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        setContentPane(root);
        setSize(800, 450);
        setLocationRelativeTo(null);
        refresh();
    }

    private void refresh() {
        try {
            int selectedIndex = chemicalBox.getSelectedIndex();
            chemicalBox.removeAllItems();
            for (Chemical c : chemicalService.getAll()) {
                chemicalBox.addItem(c);
            }
            if (selectedIndex >= 0 && selectedIndex < chemicalBox.getItemCount()) {
                chemicalBox.setSelectedIndex(selectedIndex);
            }

            model.setRowCount(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Transaction t : inventoryService.getTransactions()) {
                model.addRow(new Object[]{t.getId(), t.getDate().format(fmt), t.getChemicalName(),
                        t.getType(), Format.qty(t.getQuantity()), t.getUserName()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void change(boolean isAdd) {
        Chemical c = (Chemical) chemicalBox.getSelectedItem();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Select a chemical first.");
            return;
        }
        try {
            double amount;
            try {
                amount = Double.parseDouble(amountField.getText().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Amount must be a number.");
            }
            if (amount <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero.");
            }
            if (isAdd) {
                inventoryService.addStock(c.getId(), amount, admin.getId());
            } else {
                inventoryService.releaseStock(c.getId(), amount, admin.getId());
            }
            amountField.setText("");
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot update inventory",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}