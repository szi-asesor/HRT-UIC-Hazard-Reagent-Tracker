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
import model.ChemicalRequest;
import model.Teacher;
import service.RequestService;

/** Teacher screen: request history. PENDING requests can be edited or cancelled. */
public class MyRequestsFrame extends JFrame {
    private final Teacher teacher;
    private final RequestService service = new RequestService();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Class", "Experiment", "Date", "Status", "Reason"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextArea details = new JTextArea(8, 40);
    private final JButton editBtn = new JButton("Edit Request");
    private final JButton cancelBtn = new JButton("Cancel Request");
    private List<ChemicalRequest> requests = new ArrayList<>();

    public MyRequestsFrame(Teacher teacher) {
        this.teacher = teacher;
        setTitle("My Requests");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        details.setEditable(false);
        editBtn.setEnabled(false);
        cancelBtn.setEnabled(false);

        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
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
                editBtn.setEnabled(r.isPending());     // only PENDING requests can be changed
                cancelBtn.setEnabled(r.isPending());
            }
        });
        editBtn.addActionListener(e -> edit());
        cancelBtn.addActionListener(e -> cancel());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> load());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(refreshBtn);
        top.add(editBtn);
        top.add(cancelBtn);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(new JScrollPane(details), BorderLayout.SOUTH);
        setContentPane(root);
        setSize(800, 520);
        setLocationRelativeTo(null);
        load();
    }

    private void load() {
        try {
            requests = service.getRequestsForTeacher(teacher.getId());
            model.setRowCount(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (ChemicalRequest r : requests) {
                model.addRow(new Object[]{r.getIdText(), r.getClassName(), r.getExperiment(),
                        r.getRequestDate().format(fmt), r.getStatus(),
                        r.getRejectionReason() == null ? "" : r.getRejectionReason()});
            }
            details.setText("");
            editBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ChemicalRequest selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : requests.get(row);
    }

    private void edit() {
        ChemicalRequest r = selected();
        if (r == null) return;
        RequestFrame frame = new RequestFrame(teacher, r);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                load();   // refresh the list after the edit window closes
            }
        });
        frame.setVisible(true);
    }

    private void cancel() {
        ChemicalRequest r = selected();
        if (r == null) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Cancel request " + r.getIdText() + "? This cannot be undone by you.",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            service.cancelRequest(teacher, r.getId());
            JOptionPane.showMessageDialog(this, "Request cancelled.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot cancel", JOptionPane.ERROR_MESSAGE);
        }
        load();
    }
}

