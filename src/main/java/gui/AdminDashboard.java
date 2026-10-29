package gui;

import gui.RoundedButton.Kind;
import gui.chart.BarChartPanel;
import gui.chart.DonutChartPanel;
import gui.chart.GroupedBarPanel;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Admin;
import model.Chemical;
import model.ChemicalRequest;
import model.Transaction;
import service.AuthenticationService;
import service.ChemicalService;
import service.DashboardService;
import service.InventoryService;
import service.RequestService;
import util.Format;

/** Maximized admin dashboard with inventory and request summaries. */
public class AdminDashboard extends JFrame {
    private final Admin admin;
    private final AuthenticationService authenticationService = new AuthenticationService();
    private final ChemicalService chemicalService = new ChemicalService();
    private final RequestService requestService = new RequestService();
    private final InventoryService inventoryService = new InventoryService();
    private final DashboardService dashboardService = new DashboardService();
    private final JLabel[] values = new JLabel[6];
    private final DefaultTableModel stockModel = new DefaultTableModel(
            new Object[] { "Chemical", "Current Stock", "Minimum Stock" }, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable stockTable = new JTable(stockModel);
    private final DonutChartPanel healthChart = new DonutChartPanel();
    private final BarChartPanel hazardChart = new BarChartPanel(false);
    private final DonutChartPanel requestChart = new DonutChartPanel();
    private final GroupedBarPanel movementChart = new GroupedBarPanel();
    private final BarChartPanel requestedChart = new BarChartPanel(false);
    private final DefaultTableModel attentionModel = new DefaultTableModel(
            new Object[] { "Item", "Problem", "Detail" }, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private boolean loading;

    public AdminDashboard(Admin admin) {
        this.admin = admin;
        Theme.styleTable(stockTable);
        setTitle("Hazard & Reagent Tracker - Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(buildWindow());
        setMinimumSize(new Dimension(1100, 700));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        addWindowListener(new WindowAdapter() {
            @Override public void windowActivated(WindowEvent event) { refreshStats(); }
        });
    }

    private JPanel buildWindow() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(new JScrollPane(buildContent()), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(Theme.CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)));
        JLabel title = new JLabel("Hazard & Reagent Tracker"), user = new JLabel("Admin: " + admin.getName());
        title.setFont(Theme.TITLE); title.setForeground(Theme.TEXT);
        user.setFont(Theme.BODY); user.setForeground(Theme.MUTED);
        JPanel identity = new JPanel(new BorderLayout(16, 0));
        identity.setOpaque(false);
        identity.add(title, BorderLayout.WEST);
        identity.add(user, BorderLayout.EAST);
        header.add(identity, BorderLayout.CENTER);
        header.add(logoutButton(), BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.CARD);
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 24, 16));
        String[] labels = { "Chemicals", "Add Chemical", "Add Teacher", "Requests", "Inventory" };
        Runnable[] actions = {
            () -> new ChemicalFrame(admin, false).setVisible(true), this::addChemical, this::addTeacher,
            () -> new RequestManagementFrame(admin).setVisible(true),
            () -> new InventoryFrame(admin).setVisible(true)
        };
        for (int i = 0; i < labels.length; i++) addNavigation(sidebar, labels[i], actions[i]);
        return sidebar;
    }

    private void addNavigation(JPanel sidebar, String text, Runnable action) {
        RoundedButton button = new RoundedButton(text, Kind.SECONDARY);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(188, 42));
        button.addActionListener(event -> action.run());
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(12));
    }

    private RoundedButton logoutButton() {
        RoundedButton button = new RoundedButton("Logout", Kind.SECONDARY);
        button.addActionListener(event -> {
            for (Window window : Window.getWindows()) {
                if (window != null && window.isDisplayable()) window.dispose();
            }
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });
        return button;
    }

    private void addChemical() {
        ChemicalFormDialog dialog = new ChemicalFormDialog(this, null, admin.getId(), chemicalService);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshStats();
            JOptionPane.showMessageDialog(this, "Chemical added.");
        }
    }

    private void addTeacher() {
        TeacherFormDialog dialog = new TeacherFormDialog(this, authenticationService);
        dialog.setVisible(true);
        if (dialog.isSaved()) JOptionPane.showMessageDialog(this, "Teacher added.");
    }

    private JPanel buildContent() {
        JPanel content = new JPanel();
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(statCards());
        content.add(chartCards());
        content.add(attentionTable());
        return content;
    }

    private JPanel chartCards() {
        JPanel charts = new JPanel(new GridLayout(2, 3, 16, 16));
        charts.setOpaque(false);
        String[] titles = { "Stock vs Minimum", "Inventory Health", "Chemicals by Hazard Type",
                "Requests by Status", "Stock Movement", "Most Requested Chemicals" };
        String[] captions = { "Each chemical is compared only with its own threshold",
                "How healthy is the stock?",
                "Which hazard classifications occur most?", "Where are requests right now?",
                "Recorded additions vs releases, by week", "Chemicals included in the most requests" };
        JComponent[] panels = { new JScrollPane(stockTable), healthChart, hazardChart,
                requestChart, movementChart, requestedChart };
        for (int i = 0; i < panels.length; i++) charts.add(Theme.card(titles[i], captions[i], panels[i]));
        return charts;
    }

    private JPanel statCards() {
        String[] labels = { "Total Chemicals", "Low Stock", "Expiring Soon", "Expired", "Pending Requests",
                "<html>Approved This Month<br>(by Request Date)</html>" };
        Color[] colors = { Theme.PRIMARY, Theme.WARN, Theme.WARN, Theme.DANGER, Theme.INFO, Theme.OK };
        JPanel cards = new JPanel(new GridLayout(1, values.length, 16, 0));
        cards.setOpaque(false);
        for (int i = 0; i < values.length; i++) {
            values[i] = new JLabel("—", SwingConstants.CENTER);
            cards.add(Theme.statCard(labels[i], values[i], colors[i]));
        }
        cards.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        return cards;
    }

    private JPanel attentionTable() {
        JTable table = new JTable(attentionModel);
        Theme.styleTable(table);
        JScrollPane pane = new JScrollPane(table);
        pane.setPreferredSize(new Dimension(700, 240));
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        JLabel title = new JLabel("Needs Attention");
        title.setFont(Theme.SECTION); title.setForeground(Theme.TEXT);
        section.add(title, BorderLayout.NORTH);
        section.add(pane, BorderLayout.CENTER);
        return section;
    }

    private void refreshStats() {
        if (loading) return;
        loading = true;
        new SwingWorker<DashboardData, Void>() {
            @Override protected DashboardData doInBackground() throws Exception {
                return new DashboardData(chemicalService.getAll(), requestService.getAllRequests(),
                        inventoryService.getTransactions());
            }

            @Override protected void done() {
                loading = false;
                try {
                    DashboardData data = get();
                    updateCards(data.chemicals(), data.requests());
                    updateCharts(data.chemicals(), data.requests(), data.transactions());
                    updateAttention(data.chemicals(), data.requests());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminDashboard.this,
                            "Dashboard data could not be loaded. Check the database connection.\nDetails: "
                                    + rootMessage(ex),
                            "Unable to refresh dashboard", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception.getCause();
        return cause == null ? exception.getMessage() : cause.getMessage();
    }

    private record DashboardData(List<Chemical> chemicals, List<ChemicalRequest> requests,
            List<Transaction> transactions) { }

    private void updateCards(List<Chemical> chemicals, List<ChemicalRequest> requests) {
        YearMonth month = YearMonth.now();
        long approved = requests.stream().filter(r -> ChemicalRequest.APPROVED.equals(r.getStatus())
                && r.getRequestDate() != null && YearMonth.from(r.getRequestDate()).equals(month)).count();
        long[] counts = { chemicals.size(), chemicalService.countLowStock(chemicals),
                chemicals.stream().filter(Chemical::isExpiringSoon).count(),
                chemicalService.countExpired(chemicals), requestService.count(requests, ChemicalRequest.PENDING),
                approved };
        for (int i = 0; i < values.length; i++) values[i].setText(String.valueOf(counts[i]));
        values[5].setToolTipText("Approved requests created this month (request-date proxy; no approval timestamp exists).");
        values[5].getAccessibleContext().setAccessibleDescription(
                "Approved requests created this month, using request date because approval time is not stored.");
    }

    private void updateCharts(List<Chemical> chemicals, List<ChemicalRequest> requests,
            List<Transaction> transactions) {
        updateStock(chemicals);
        setDonut(healthChart, dashboardService.inventoryHealth(chemicals),
                Arrays.asList(Theme.OK, Theme.WARN, Theme.WARN, Theme.DANGER));
        setDonut(requestChart, dashboardService.requestStatusCounts(requests),
                Arrays.asList(Theme.INFO, Theme.OK, Theme.DANGER, Theme.GREY));
        setBars(hazardChart, dashboardService.hazardCounts(chemicals), null);
        setMovement(transactions);
        List<ChemicalRequest> active = requests.stream().filter(r -> ChemicalRequest.APPROVED.equals(r.getStatus())
                || ChemicalRequest.PENDING.equals(r.getStatus())).collect(Collectors.toList());
        setBars(requestedChart, dashboardService.topRequestedChemicals(active, 5), null);
    }

    private void updateStock(List<Chemical> chemicals) {
        stockModel.setRowCount(0);
        for (Chemical chemical : dashboardService.lowestStock(chemicals, 8)) {
            String unit = chemical.getUnit() == null ? "" : " " + chemical.getUnit();
            stockModel.addRow(new Object[] { chemical.getName(),
                    Format.qty(chemical.getQuantity()) + unit,
                    Format.qty(chemical.getMinimumStock()) + unit });
        }
    }

    private void setDonut(DonutChartPanel chart, Map<String, Integer> data, List<Color> colors) {
        List<String> labels = new ArrayList<>(data.keySet());
        chart.setData(labels, labels.stream().map(k -> data.get(k).doubleValue()).collect(Collectors.toList()), colors);
    }

    private void setBars(BarChartPanel chart, Map<String, ? extends Number> data, List<Color> colors) {
        List<String> labels = new ArrayList<>(data.keySet());
        chart.setData(labels, labels.stream().map(k -> data.get(k).doubleValue()).collect(Collectors.toList()), colors);
    }

    private void setMovement(List<Transaction> transactions) {
        LocalDate cutoff = LocalDate.now().minusDays(30);
        List<Transaction> recent = transactions.stream().filter(t -> t.getDate() != null
                && !t.getDate().toLocalDate().isBefore(cutoff)
                && ("ADD".equalsIgnoreCase(t.getType()) || "RELEASE".equalsIgnoreCase(t.getType())))
                .collect(Collectors.toList());
        if (recent.isEmpty()) {
            movementChart.setData(Collections.emptyList(), "ADD", Collections.emptyList(), "RELEASE", Collections.emptyList());
            return;
        }
        Map<String, double[]> data = dashboardService.movementByWeek(recent, 6);
        List<String> weeks = new ArrayList<>(data.keySet());
        movementChart.setData(weeks, "ADD", weeks.stream().map(k -> data.get(k)[0]).collect(Collectors.toList()),
                "RELEASE", weeks.stream().map(k -> data.get(k)[1]).collect(Collectors.toList()));
    }

    private void updateAttention(List<Chemical> chemicals, List<ChemicalRequest> requests) {
        List<Object[]> rows = new ArrayList<>();
        addChemicalAttention(rows, chemicals);
        addPendingAttention(rows, requests);
        rows.sort(Comparator.comparingInt(row -> attentionRank((String) row[1])));
        attentionModel.setRowCount(0);
        if (rows.isEmpty()) attentionModel.addRow(new Object[] { "Everything looks good.", "", "" });
        else rows.stream().limit(8).forEach(attentionModel::addRow);
    }

    private void addChemicalAttention(List<Object[]> rows, List<Chemical> chemicals) {
        for (Chemical chemical : chemicals) {
            if (chemical.isExpired()) rows.add(new Object[] { chemical.getName(), "EXPIRED",
                    "Expired " + chemical.getExpirationDate() });
            else if (chemical.isLowStock()) rows.add(new Object[] { chemical.getName(), "LOW STOCK",
                    chemical.getQuantity() + " " + chemical.getUnit() + " (minimum " + chemical.getMinimumStock() + ")" });
            else if (chemical.isExpiringSoon()) rows.add(new Object[] { chemical.getName(), "EXPIRING SOON",
                    "Expires " + chemical.getExpirationDate() });
        }
    }

    private void addPendingAttention(List<Object[]> rows, List<ChemicalRequest> requests) {
        for (ChemicalRequest request : requests) if (ChemicalRequest.PENDING.equals(request.getStatus()))
            rows.add(new Object[] { "Request " + request.getIdText(), "PENDING REQUEST",
                    request.getTeacherName() + " · " + request.getClassName() });
    }

    private int attentionRank(String problem) {
        return switch (problem) {
            case "EXPIRED" -> 0;
            case "LOW STOCK" -> 1;
            case "EXPIRING SOON" -> 2;
            default -> 3;
        };
    }
}
