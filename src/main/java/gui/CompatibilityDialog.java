package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import service.ChemicalService;

/** Read-only view of the lab's fixed chemical segregation rules. */
public class CompatibilityDialog extends JDialog {
    public CompatibilityDialog(JFrame owner, ChemicalService service) {
        super(owner, "Fixed Storage Safety Rules", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTextArea explanation = new JTextArea(
                "These storage segregation rules are fixed and cannot be edited in the tracker. "
                + "DOLE Department Order No. 136-14, Section 6(4), requires chemicals to be properly "
                + "segregated according to GHS physical-hazard criteria and the storage requirements "
                + "in each chemical's Safety Data Sheet (SDS). The specific pairs below are the lab's "
                + "operational rules; consult the SDS for the controlling storage instructions.\n\n"
                + "Official text: https://elibrary.judiciary.gov.ph/thebookshelf/showdocs/10/68235");
        explanation.setColumns(60);
        explanation.setRows(5);
        explanation.setFont(Theme.BODY);
        explanation.setForeground(Theme.TEXT);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);
        explanation.setEditable(false);
        explanation.setOpaque(false);
        explanation.setBorder(BorderFactory.createEmptyBorder(4, 4, 12, 4));

        DefaultTableModel model = new DefaultTableModel(new Object[] {"Storage class", "Keep separate from"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (String[] pair : service.getRules()) {
            model.addRow(new Object[] {label(pair[0]), label(pair[1])});
        }
        JTable table = new JTable(model);
        Theme.styleTable(table);
        table.setPreferredScrollableViewportSize(new Dimension(620, 190));

        RoundedButton close = new RoundedButton("Close", RoundedButton.Kind.SECONDARY);
        close.addActionListener(event -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(close);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.add(explanation, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }

    private String label(String group) {
        return switch (group) {
            case "Acid" -> "Acid Corrosive";
            case "Base" -> "Base Corrosive";
            case "Cyanide" -> "Cyanide (hazard)";
            case "Organic Material" -> "Organic Material (hazard)";
            default -> group;
        };
    }
}
