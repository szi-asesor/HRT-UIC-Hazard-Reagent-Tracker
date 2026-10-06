package gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Chemical;
import model.User;
import service.ChemicalService;
import util.Format;

/**
 * Chemical list with search. Admin sees Add / Edit / Remove / Rules buttons.
 * Teacher sees the same table read-only (readOnly = true) to view hazard information.
 */
public class ChemicalFrame extends JFrame {
    private static final String[] COLUMNS = {"Code", "Name", "Quantity", "Unit", "Hazard", "Group",
            "Temperature", "Location", "Min Stock", "Expires", "Status"};

    private final User user;
    private final ChemicalService service = new ChemicalService();
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField searchField = new JTextField(15);
    private List<Chemical> shown = new ArrayList<>();

    public ChemicalFrame(User user, boolean readOnly) {
        this.user = user;
        setTitle(readOnly ? "Available Chemicals" : "Manage Chemicals");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JButton searchBtn = new JButton("Search");
        JButton showAllBtn = new JButton("Show All");
        searchBtn.addActionListener(e -> load(searchField.getText()));
        showAllBtn.addActionListener(e -> {
            searchField.setText("");
            load("");
        });
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search name / hazard:"));
        top.add(searchField);
        top.add(searchBtn);
        top.add(showAllBtn);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        if (!readOnly) {
            JButton addBtn = new JButton("Add");
            JButton editBtn = new JButton("Edit");
            JButton removeBtn = new JButton("Remove");
            JButton rulesBtn = new JButton("Compatibility Rules");
            addBtn.addActionListener(e -> openForm(null));
            editBtn.addActionListener(e -> {
                Chemical c = selected();
                if (c != null) openForm(c);
            });
            removeBtn.addActionListener(e -> remove());
            rulesBtn.addActionListener(e -> new CompatibilityDialog(this, service).setVisible(true));
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
            bottom.add(addBtn);
            bottom.add(editBtn);
            bottom.add(removeBtn);
            bottom.add(rulesBtn);
            root.add(bottom, BorderLayout.SOUTH);
        }

        setContentPane(root);
        setSize(1000, 450);
        setLocationRelativeTo(null);
        load("");
    }

    private void load(String keyword) {
        try {
            shown = service.search(keyword);
            model.setRowCount(0);
            for (Chemical c : shown) {
                model.addRow(new Object[]{c.getCode(), c.getName(), Format.qty(c.getQuantity()), c.getUnit(),
                        c.getHazardType(), c.getStorageGroup(), c.getStorageTemperature(), c.getLocation(),
                        Format.qty(c.getMinimumStock()),
                        c.getExpirationDate() == null ? "" : c.getExpirationDate().toString(),
                        c.getStatus()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Chemical selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a chemical first.");
            return null;
        }
        return shown.get(row);
    }

    private void openForm(Chemical existing) {
        ChemicalFormDialog dialog = new ChemicalFormDialog(this, existing, user.getId(), service);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            load("");
        }
    }

    private void remove() {
        Chemical c = selected();
        if (c == null) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Remove '" + c.getName() + "' from the active list?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            service.deactivate(c.getId());
            load("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
