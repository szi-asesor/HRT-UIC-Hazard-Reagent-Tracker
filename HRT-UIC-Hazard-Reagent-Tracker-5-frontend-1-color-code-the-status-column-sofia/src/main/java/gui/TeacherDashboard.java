package gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.*;
import model.ChemicalRequest;
import model.Teacher;
import service.RequestService;

/** Main screen for the Teacher: their request counts and buttons. */
public class TeacherDashboard extends JFrame {
    private final Teacher teacher;
    private final RequestService requestService = new RequestService();

    private final JLabel totalLabel = new JLabel();
    private final JLabel pendingLabel = new JLabel();
    private final JLabel approvedLabel = new JLabel();
    private final JLabel rejectedLabel = new JLabel();
    private final JLabel cancelledLabel = new JLabel();

    public TeacherDashboard(Teacher teacher) {
        this.teacher = teacher;
        setTitle("Hazard & Reagent Tracker - Teacher");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel title = new JLabel("Welcome, " + teacher.getName(), SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel stats = new JPanel(new GridLayout(5, 1, 4, 4));
        stats.add(totalLabel);
        stats.add(pendingLabel);
        stats.add(approvedLabel);
        stats.add(rejectedLabel);
        stats.add(cancelledLabel);

        JPanel buttons = new JPanel(new GridLayout(4, 1, 6, 6));
        JButton newBtn = new JButton("New Request");
        JButton myBtn = new JButton("My Requests");
        JButton chemBtn = new JButton("Chemicals");
        JButton logoutBtn = new JButton("Logout");
        buttons.add(newBtn);
        buttons.add(myBtn);
        buttons.add(chemBtn);
        buttons.add(logoutBtn);

        newBtn.addActionListener(e -> new RequestFrame(teacher).setVisible(true));
        myBtn.addActionListener(e -> new MyRequestsFrame(teacher).setVisible(true));
        chemBtn.addActionListener(e -> new ChemicalFrame(teacher, true).setVisible(true));   // read-only
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
        setSize(480, 300);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                refreshStats();
            }
        });
    }

    private void refreshStats() {
        try {
            List<ChemicalRequest> list = requestService.getRequestsForTeacher(teacher.getId());
            totalLabel.setText("My Requests: " + list.size());
            pendingLabel.setText("Pending: " + requestService.count(list, ChemicalRequest.PENDING));
            approvedLabel.setText("Approved: " + requestService.count(list, ChemicalRequest.APPROVED));
            rejectedLabel.setText("Rejected: " + requestService.count(list, ChemicalRequest.REJECTED));
            cancelledLabel.setText("Cancelled: " + requestService.count(list, ChemicalRequest.CANCELLED));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
