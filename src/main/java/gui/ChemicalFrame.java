package gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
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

        Theme.styleInput(searchField);
        Theme.styleTable(table);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(10).setCellRenderer(new StatusRenderer());
        addWindowListener(new WindowAdapter() {
            @Override public void windowGainedFocus(WindowEvent event) { load(searchField.getText()); }
        });

        RoundedButton searchBtn = new RoundedButton("Search", RoundedButton.Kind.SECONDARY);
        RoundedButton showAllBtn = new RoundedButton("Show All", RoundedButton.Kind.SECONDARY);
        searchBtn.addActionListener(e -> load(searchField.getText()));
        showAllBtn.addActionListener(e -> {
            searchField.setText("");
            load("");
        });
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search code / name / hazard:"));
        top.add(searchField);
        top.add(searchBtn);
        top.add(showAllBtn);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        if (!readOnly) {
            RoundedButton addBtn = new RoundedButton("Add", RoundedButton.Kind.PRIMARY);
            RoundedButton editBtn = new RoundedButton("Edit", RoundedButton.Kind.SECONDARY);
            RoundedButton removeBtn = new RoundedButton("Remove", RoundedButton.Kind.DANGER);
            RoundedButton rulesBtn = new RoundedButton("Storage Safety Rules", RoundedButton.Kind.SECONDARY);
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
        setSize(Theme.screenPercent(0.9));
        setLocationRelativeTo(null);
        load("");
    }

    private void load(String keyword) {
        try {
            shown = service.search(keyword);
            model.setRowCount(0);
            for (Chemical c : shown) {
                model.addRow(new Object[]{c.getCode(), c.getName(), Format.qty(c.getQuantity()), c.getUnit(),
                        formatHazards(c.getHazardType()), c.getStorageGroup(), c.getStorageTemperature(),
                        ChemicalService.canonicalStorageLocation(c.getLocation(), c.getStorageGroup()),
                        Format.qty(c.getMinimumStock()),
                        c.getExpirationDate() == null ? "" : c.getExpirationDate().toString(),
                        c.getStatus()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatHazards(String hazards) {
        return hazards == null ? "None" : hazards.replaceAll("\\s*,\\s*", ", ");
    }

    private Chemical selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a chemical first.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return shown.get(modelRow);
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

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable source, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(source, value, selected, focused, row, column);
            setFont(Theme.TABLE);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            if (selected) {
                setForeground(java.awt.Color.WHITE);
            } else {
                String status = value == null ? "" : value.toString().toUpperCase();
                setForeground(status.contains("EXPIRED") ? Theme.DANGER
                        : status.contains("LOW") || status.contains("EXPIR") ? Theme.WARN
                        : status.equals("OK") ? Theme.OK : Theme.TEXT);
            }
            setBackground(selected ? Theme.PRIMARY : (row % 2 == 0 ? Theme.CARD : new java.awt.Color(0xF9FAFB)));
            return this;
        }
    }
}
