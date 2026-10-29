package gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import model.Admin;
import model.ChemicalRequest;
import service.RequestService;

/** Admin screen: review and approve requests, or ask about incompatible chemical pairs. */
public class RequestManagementFrame extends JFrame {
    private final Admin admin;
    private final RequestService service = new RequestService();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Teacher", "Class", "Experiment", "Date", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JComboBox<String> statusFilterCombo = new JComboBox<>(new String[]{
            "ALL", ChemicalRequest.PENDING, ChemicalRequest.APPROVED, ChemicalRequest.REJECTED, ChemicalRequest.CANCELLED
    });
    private final JTextArea details = new JTextArea(9, 40);
    private final RoundedButton approveBtn = new RoundedButton("Approve", RoundedButton.Kind.PRIMARY);
    private final RoundedButton clarifyBtn = new RoundedButton("Ask Teacher About Safety", RoundedButton.Kind.SECONDARY);
    private final RoundedButton statusBtn = new RoundedButton("Change Status...", RoundedButton.Kind.SECONDARY);
    private List<ChemicalRequest> requests = new ArrayList<>();

    public RequestManagementFrame(Admin admin) {
        this.admin = admin;
        setTitle("Manage Requests");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Theme.styleTable(table);
        table.setRowSorter(sorter);
        details.setFont(Theme.BODY);
        addWindowListener(new WindowAdapter() {
            @Override public void windowGainedFocus(WindowEvent event) { load(); }
        });
        statusFilterCombo.addActionListener(event -> applyFilter());
        details.setEditable(false);
        setButtons(null);

        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value != null) {
                    String status = value.toString().toUpperCase();
                    switch (status) {
                        case "APPROVED":
                        case "AVAILABLE":
                        case "ACTIVE":
                            c.setBackground(new Color(200, 240, 200));
                            c.setForeground(Color.BLACK);
                            break;
                        case "PENDING":
                        case "LOW STOCK":
                            c.setBackground(new Color(255, 243, 205));
                            c.setForeground(Color.BLACK);
                            break;
                        case "REJECTED":
                        case "OUT OF STOCK":
                        case "HAZARDOUS":
                        case "CANCELLED":
                            c.setBackground(new Color(255, 204, 204));
                            c.setForeground(Color.BLACK);
                            break;
                        default:
                            c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                            c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                            break;
                    }
                }

                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                }

                return c;
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && viewRow >= 0) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                ChemicalRequest r = requests.get(modelRow);
                List<String> conflicts = service.incompatiblePairs(r);
                details.setText(r.getDetailsText() + (conflicts.isEmpty() ? ""
                        : "\nSAFETY CLARIFICATION NEEDED:\n" + String.join("\n", conflicts)));
                setButtons(r);
            }
        });
        approveBtn.addActionListener(e -> apply(ChemicalRequest.APPROVED));
        clarifyBtn.addActionListener(e -> askForClarification());
        statusBtn.addActionListener(e -> chooseStatus());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Filter Status:"));
        top.add(statusFilterCombo);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(approveBtn);
        buttons.add(clarifyBtn);
        buttons.add(statusBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(new JScrollPane(details), BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(Theme.screenPercent(0.9));
        setLocationRelativeTo(null);
        load();
    }

    private void applyFilter() {
        String selected = (String) statusFilterCombo.getSelectedItem();
        if (selected == null || "ALL".equals(selected)) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)^" + selected + "$", 5));
        }
    }

    /** Actions are disabled for requests the teacher has cancelled. */
    private void setButtons(ChemicalRequest r) {
        boolean active = r != null && !ChemicalRequest.CANCELLED.equals(r.getStatus());
        approveBtn.setEnabled(active && !ChemicalRequest.APPROVED.equals(r.getStatus()));
        clarifyBtn.setEnabled(r != null && ChemicalRequest.PENDING.equals(r.getStatus())
                && !service.incompatiblePairs(r).isEmpty());
        statusBtn.setEnabled(active);
    }

    private void load() {
        try {
            requests = service.getAllRequests();
            model.setRowCount(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (ChemicalRequest r : requests) {
                model.addRow(new Object[]{r.getIdText(), r.getTeacherName(), r.getClassName(),
                        r.getExperiment(), r.getRequestDate().format(fmt), r.getStatus()});
            }
            details.setText("");
            setButtons(null);
            applyFilter();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ChemicalRequest selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        return requests.get(modelRow);
    }

    private void chooseStatus() {
        ChemicalRequest r = selected();
        if (r == null) return;
        String[] options = {ChemicalRequest.PENDING, ChemicalRequest.APPROVED, ChemicalRequest.CANCELLED};
        String initial = List.of(options).contains(r.getStatus()) ? r.getStatus() : ChemicalRequest.PENDING;
        String choice = (String) JOptionPane.showInputDialog(this, "New status for request " + r.getIdText() + ":",
                "Change Status", JOptionPane.QUESTION_MESSAGE, null, options, initial);
        if (choice != null) {
            apply(choice);
        }
    }

    /** One method handles every status change, so the rules stay in RequestService. */
    private void apply(String newStatus) {
        ChemicalRequest r = selected();
        if (r == null) return;

        String old = r.getStatus();
        if (ChemicalRequest.CANCELLED.equals(old)) {
            JOptionPane.showMessageDialog(this, "Teacher-cancelled requests are final and cannot be changed.",
                    "Request is cancelled", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String note = "";
        if (newStatus.equals(ChemicalRequest.APPROVED) && !old.equals(ChemicalRequest.APPROVED)) {
            note += "\n- The chemicals will be RELEASED from inventory.";
        }
        if (old.equals(ChemicalRequest.APPROVED) && !newStatus.equals(ChemicalRequest.APPROVED)) {
            note += "\n- The released chemicals will be RETURNED to inventory.";
        }
        boolean emails = newStatus.equals(ChemicalRequest.APPROVED);
        if (emails) {
            note += "\n- The teacher will be emailed.";
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Change request " + r.getIdText() + " from " + old + " to " + newStatus + "?" + note,
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;

        try {
            String emailProblem = service.changeStatus(r.getId(), newStatus, admin.getId());
            if (emailProblem == null) {
                JOptionPane.showMessageDialog(this, "Status changed to " + newStatus + "."
                        + (emails ? " Email sent to the teacher." : ""));
            } else {
                JOptionPane.showMessageDialog(this, emailProblem, "Email error", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot change status", JOptionPane.ERROR_MESSAGE);
        }
        load();
    }

    private void askForClarification() {
        ChemicalRequest request = selected();
        if (request == null) return;
        List<String> pairs = service.incompatiblePairs(request);
        if (pairs.isEmpty()) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Email the teacher to explain why these incompatible chemicals were requested together?\n\n"
                        + String.join("\n", pairs) + "\n\nThe request will remain PENDING.",
                "Ask for Safety Clarification", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            String problem = service.askForSafetyClarification(request.getId());
            JOptionPane.showMessageDialog(this, problem == null
                    ? "Safety clarification email sent. The request remains PENDING." : problem,
                    problem == null ? "Email sent" : "Email not sent",
                    problem == null ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Cannot send clarification",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
