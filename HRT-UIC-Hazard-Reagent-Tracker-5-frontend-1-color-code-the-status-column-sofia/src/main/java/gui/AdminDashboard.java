package gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.*;
import model.Admin;
import model.Chemical;
import model.ChemicalRequest;
import service.ChemicalService;
import service.RequestService;

/** Main screen for the Admin: summary numbers and buttons. */
public class AdminDashboard extends JFrame {
    private final Admin admin;
    private final ChemicalService chemicalService = new ChemicalService();
    private final RequestService requestService = new RequestService();

    private final JLabel chemicalsLabel = new JLabel();
    private final JLabel lowStockLabel = new JLabel();
    private final JLabel pendingLabel = new JLabel();
    private final JLabel expiredLabel = new JLabel();

    public AdminDashboard(Admin admin) {
        this.admin = admin;
        setTitle("Hazard & Reagent Tracker - Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel title = new JLabel("Hazard & Reagent Tracker  (Admin: " + admin.getName() + ")", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel stats = new JPanel(new GridLayout(4, 1, 4, 4));
        stats.add(chemicalsLabel);
        stats.add(lowStockLabel);
        stats.add(pendingLabel);
        stats.add(expiredLabel);

        JPanel buttons = new JPanel(new GridLayout(5, 1, 6, 6));
        JButton chemicalsBtn = new JButton("Chemicals");
        JButton addChemicalBtn = new JButton("Add Chemical");
        JButton requestsBtn = new JButton("Requests");
        JButton inventoryBtn = new JButton("Inventory");
        JButton logoutBtn = new JButton("Logout");
        buttons.add(chemicalsBtn);
        buttons.add(addChemicalBtn);
        buttons.add(requestsBtn);
        buttons.add(inventoryBtn);
        buttons.add(logoutBtn);

        chemicalsBtn.addActionListener(e -> new ChemicalFrame(admin, false).setVisible(true));
        addChemicalBtn.addActionListener(e -> {
            ChemicalFormDialog dialog = new ChemicalFormDialog(this, null, admin.getId(), chemicalService);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshStats();
                JOptionPane.showMessageDialog(this, "Chemical added.");
            }
        });
        requestsBtn.addActionListener(e -> new RequestManagementFrame(admin).setVisible(true));
        inventoryBtn.addActionListener(e -> new InventoryFrame(admin).setVisible(true));
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        root.add(title, BorderLayout.NORTH);
        root.add(stats, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.EAST);
        setContentPane(root);
        setSize(520, 300);
        setLocationRelativeTo(null);

        // refresh the numbers whenever this window comes back to the front
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                refreshStats();
            }
        });
    }

    private void refreshStats() {
        try {
            List<Chemical> chemicals = chemicalService.getAll();
            List<ChemicalRequest> requests = requestService.getAllRequests();
            chemicalsLabel.setText("Chemicals: " + chemicals.size());
            lowStockLabel.setText("Low Stock: " + chemicalService.countLowStock(chemicals));
            pendingLabel.setText("Pending Requests: " + requestService.count(requests, ChemicalRequest.PENDING));
            expiredLabel.setText("Expired: " + chemicalService.countExpired(chemicals));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
