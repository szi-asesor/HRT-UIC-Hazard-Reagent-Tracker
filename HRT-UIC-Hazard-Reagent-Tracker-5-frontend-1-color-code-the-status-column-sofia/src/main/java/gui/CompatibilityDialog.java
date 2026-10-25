package gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import service.ChemicalService;

/**
 * Admin screen to view, add, EDIT and DELETE "incompatible group" rules (e.g. Acid + Base).
 * Select a rule in the list: its two groups appear in the boxes so you can change them and press Update.
 */
public class CompatibilityDialog extends JDialog {
    private static final String[] GROUPS = {"Acid", "Base", "Oxidizer", "Flammable", "Other"};

    private final ChemicalService service;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> list = new JList<>(listModel);
    private final JComboBox<String> groupA = new JComboBox<>(GROUPS);
    private final JComboBox<String> groupB = new JComboBox<>(GROUPS);
    private List<String[]> rules = new ArrayList<>();

    public CompatibilityDialog(JFrame owner, ChemicalService service) {
        super(owner, "Incompatible Storage Groups", true);
        this.service = service;

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> {
            int index = list.getSelectedIndex();
            if (!e.getValueIsAdjusting() && index >= 0) {
                groupA.setSelectedItem(rules.get(index)[1]);
                groupB.setSelectedItem(rules.get(index)[2]);
            }
        });

        JButton addBtn = new JButton("Add Rule");
        JButton updateBtn = new JButton("Update Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        addBtn.addActionListener(e -> addRule());
        updateBtn.addActionListener(e -> updateRule());
        deleteBtn.addActionListener(e -> deleteRule());

        JPanel groups = new JPanel();
        groups.add(groupA);
        groups.add(new JLabel("+"));
        groups.add(groupB);
        JPanel buttons = new JPanel();
        buttons.add(addBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.add(groups);
        bottom.add(buttons);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(new JLabel("These groups must NOT be stored together:"), BorderLayout.NORTH);
        root.add(new JScrollPane(list), BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(520, 380);
        setLocationRelativeTo(owner);
        load();
    }

    private void load() {
        try {
            rules = service.getRules();
            listModel.clear();
            for (String[] r : rules) {
                listModel.addElement(r[1] + " + " + r[2] + "  →  Incompatible");
            }
        } catch (Exception ex) {
            showError(ex, "Error");
        }
    }

    private void addRule() {
        try {
            service.addRule((String) groupA.getSelectedItem(), (String) groupB.getSelectedItem());
            load();
        } catch (Exception ex) {
            showError(ex, "Cannot add rule");
        }
    }

    private void updateRule() {
        int index = list.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Select a rule first, then change the groups.");
            return;
        }
        try {
            service.updateRule(Integer.parseInt(rules.get(index)[0]),
                    (String) groupA.getSelectedItem(), (String) groupB.getSelectedItem());
            load();
        } catch (Exception ex) {
            showError(ex, "Cannot update rule");
        }
    }

    private void deleteRule() {
        int index = list.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Select a rule first.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Delete the rule '" + listModel.get(index) + "'?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            service.deleteRule(Integer.parseInt(rules.get(index)[0]));
            load();
        } catch (Exception ex) {
            showError(ex, "Error");
        }
    }

    private void showError(Exception ex, String title) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}
