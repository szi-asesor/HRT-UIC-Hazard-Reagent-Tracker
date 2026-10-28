package gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Chemical;
import model.ChemicalRequest;
import model.RequestItem;
import model.Teacher;
import service.ChemicalService;
import service.RequestService;
import util.Format;

/**
 * Teacher screen: build one request that contains several chemicals, then submit it.
 * If an existing PENDING request is passed in, the same screen is used to EDIT it.
 */
public class RequestFrame extends JFrame {
    private final Teacher teacher;
    private final ChemicalRequest existing;   // null = new request
    private final ChemicalService chemicalService = new ChemicalService();
    private final RequestService requestService = new RequestService();
    private final List<RequestItem> items = new ArrayList<>();

    private final JTextField classField = new JTextField(15);
    private final JTextField experimentField = new JTextField(25);
    private final JComboBox<Chemical> chemicalBox = new JComboBox<>();
    private final JTextField quantityField = new JTextField(8);
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Chemical", "Quantity", "Unit"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public RequestFrame(Teacher teacher) {
        this(teacher, null);
    }

    public RequestFrame(Teacher teacher, ChemicalRequest existing) {
        this.teacher = teacher;
        this.existing = existing;
        setTitle(existing == null ? "New Chemical Request" : "Edit Request " + existing.getIdText());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Enable automatic table sorting
        table.setAutoCreateRowSorter(true);

        JPanel info = new JPanel(new GridLayout(2, 2, 6, 6));
        info.add(new JLabel("Class / Section"));
        info.add(classField);
        info.add(new JLabel("Purpose / Experiment"));
        info.add(experimentField);

        JButton addBtn = new JButton("Add Chemical");
        addBtn.addActionListener(e -> addItem());
        JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT));
        picker.add(chemicalBox);
        picker.add(new JLabel("Quantity"));
        picker.add(quantityField);
        picker.add(addBtn);

        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.add(info, BorderLayout.NORTH);
        top.add(picker, BorderLayout.SOUTH);

        JButton removeBtn = new JButton("Remove Selected");
        JButton submitBtn = new JButton(existing == null ? "Submit Request" : "Save Changes");
        removeBtn.addActionListener(e -> removeItem());
        submitBtn.addActionListener(e -> submit());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(removeBtn);
        bottom.add(submitBtn);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(700, 420);
        setLocationRelativeTo(null);
        loadChemicals();

        if (existing != null) {   // pre-fill the form for editing
            classField.setText(existing.getClassName());
            experimentField.setText(existing.getExperiment());
            for (RequestItem old : existing.getItems()) {
                items.add(new RequestItem(old.getChemicalId(), old.getChemicalName(), old.getUnit(), old.getQuantity()));
            }
            refreshTable();
        }
    }

    private void loadChemicals() {
        try {
            for (Chemical c : chemicalService.getAll()) {
                chemicalBox.addItem(c);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addItem() {
        Chemical c = (Chemical) chemicalBox.getSelectedItem();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "No chemical selected.");
            return;
        }
        double qty;
        try {
            qty = Double.parseDouble(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        // same chemical twice? just add the quantities together
        for (RequestItem item : items) {
            if (item.getChemicalId() == c.getId()) {
                item.setQuantity(item.getQuantity() + qty);
                refreshTable();
                quantityField.setText("");
                return;
            }
        }
        items.add(new RequestItem(c.getId(), c.getName(), c.getUnit(), qty));
        refreshTable();
        quantityField.setText("");
    }

    private void removeItem() {
        int viewRow = table.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            items.remove(modelRow);
            refreshTable();
        }
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (RequestItem item : items) {
            model.addRow(new Object[]{item.getChemicalName(), Format.qty(item.getQuantity()), item.getUnit()});
        }
    }

    private void submit() {
        try {
            if (existing == null) {
                requestService.createRequest(teacher, classField.getText(), experimentField.getText(), items);
                JOptionPane.showMessageDialog(this, "Request submitted. Status: PENDING");
            } else {
                requestService.updateRequest(teacher, existing.getId(), classField.getText(),
                        experimentField.getText(), items);
                JOptionPane.showMessageDialog(this, "Request updated.");
            }
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot save", JOptionPane.ERROR_MESSAGE);
        }
    }
}