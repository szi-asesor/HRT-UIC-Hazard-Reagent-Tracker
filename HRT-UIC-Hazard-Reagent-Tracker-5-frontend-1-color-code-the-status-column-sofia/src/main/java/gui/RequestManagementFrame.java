package gui;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.Admin;
import model.ChemicalRequest;
import service.RequestService;

/** Admin screen: review requests, approve / reject, or change a request to any status. */
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
    private final JTextArea details = new JTextArea(9, 40);
    private final JButton approveBtn = new JButton("Approve");
    private final JButton rejectBtn = new JButton("Reject");
    private final JButton statusBtn = new JButton("Change Status...");
    private List<ChemicalRequest> requests = new ArrayList<>();

    public RequestManagementFrame(Admin admin) {
        this.admin = admin;
        setTitle("Manage Requests");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                ChemicalRequest r = requests.get(row);
                details.setText(r.getDetailsText());
                setButtons(r);
            }
        });
        approveBtn.addActionListener(e -> apply(ChemicalRequest.APPROVED));
        rejectBtn.addActionListener(e -> apply(ChemicalRequest.REJECTED));
        statusBtn.addActionListener(e -> chooseStatus());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(approveBtn);
        buttons.add(rejectBtn);
        buttons.add(statusBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(new JScrollPane(details), BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(850, 560);
        setLocationRelativeTo(null);
        load();
    }

    /** Approve / Reject are only useful when the request is not already in that status. */
    private void setButtons(ChemicalRequest r) {
        approveBtn.setEnabled(r != null && !r.getStatus().equals(ChemicalRequest.APPROVED));
        rejectBtn.setEnabled(r != null && !r.getStatus().equals(ChemicalRequest.REJECTED));
        statusBtn.setEnabled(r != null);
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
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ChemicalRequest selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : requests.get(row);
    }

    private void chooseStatus() {
        ChemicalRequest r = selected();
        if (r == null) return;
        String[] options = {ChemicalRequest.PENDING, ChemicalRequest.APPROVED,
                ChemicalRequest.REJECTED, ChemicalRequest.CANCELLED};
        String choice = (String) JOptionPane.showInputDialog(this, "New status for request " + r.getIdText() + ":",
                "Change Status", JOptionPane.QUESTION_MESSAGE, null, options, r.getStatus());
        if (choice != null) {
            apply(choice);
        }
    }

    /** One method handles every status change, so the rules stay in RequestService. */
    private void apply(String newStatus) {
        ChemicalRequest r = selected();
        if (r == null) return;

        String old = r.getStatus();
        String note = "";
        if (newStatus.equals(ChemicalRequest.APPROVED) && !old.equals(ChemicalRequest.APPROVED)) {
            note += "\n- The chemicals will be RELEASED from inventory.";
        }
        if (old.equals(ChemicalRequest.APPROVED) && !newStatus.equals(ChemicalRequest.APPROVED)) {
            note += "\n- The released chemicals will be RETURNED to inventory.";
        }
        boolean emails = newStatus.equals(ChemicalRequest.APPROVED) || newStatus.equals(ChemicalRequest.REJECTED);
        if (emails) {
            note += "\n- The teacher will be emailed.";
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Change request " + r.getIdText() + " from " + old + " to " + newStatus + "?" + note,
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;

        String reason = null;
        if (newStatus.equals(ChemicalRequest.REJECTED)) {
            reason = JOptionPane.showInputDialog(this, "Reason for rejection:");
            if (reason == null) return;   // admin pressed Cancel
        }

        try {
            String emailProblem = service.changeStatus(r.getId(), newStatus, reason, admin.getId());
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
}