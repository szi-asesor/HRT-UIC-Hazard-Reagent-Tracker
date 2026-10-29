package gui;

import gui.chart.BarChartPanel;
import gui.chart.DonutChartPanel;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Chemical;
import model.ChemicalRequest;
import model.Teacher;
import service.ChemicalService;
import service.DashboardService;
import service.RequestService;

/** Main screen for teachers to review chemical requests and availability. */
public class TeacherDashboard extends JFrame {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private final Teacher teacher;
    private final RequestService requestService = new RequestService();
    private final ChemicalService chemicalService = new ChemicalService();
    private final DashboardService dashboardService = new DashboardService();
    private final JLabel[] statValues = {new JLabel("0"), new JLabel("0"), new JLabel("0"), new JLabel("0")};
    private final DonutChartPanel statusChart = new DonutChartPanel();
    private final BarChartPanel monthChart = new BarChartPanel(false), chemicalsChart = new BarChartPanel(false),
            availabilityChart = new BarChartPanel(true);
    private final DefaultTableModel requestRows = new DefaultTableModel(
            new Object[] {"ID", "Class", "Experiment", "Status", "Date"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private boolean loading;

    public TeacherDashboard(Teacher teacher) {
        this.teacher = teacher;
        setTitle("Hazard & Reagent Tracker - Teacher");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setContentPane(buildLayout());
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        addWindowListener(new WindowAdapter() {
            @Override public void windowActivated(WindowEvent event) { refreshDashboard(); }
        });
    }

    private JPanel buildLayout() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        JScrollPane contentScroll = new JScrollPane(buildContent());
        contentScroll.setBorder(BorderFactory.createEmptyBorder());
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(contentScroll, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(Theme.CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)));
        JLabel greeting = new JLabel("Welcome, " + teacher.getName());
        greeting.setFont(Theme.TITLE); greeting.setForeground(Theme.TEXT);
        JLabel helper = new JLabel("Request chemicals for your class and track approvals here.");
        helper.setFont(Theme.BODY); helper.setForeground(Theme.MUTED);
        JPanel identity = new JPanel();
        identity.setLayout(new javax.swing.BoxLayout(identity, javax.swing.BoxLayout.Y_AXIS));
        identity.setOpaque(false);
        identity.add(greeting);
        identity.add(helper);
        header.add(identity, BorderLayout.WEST);
        RoundedButton logout = new RoundedButton("Logout", RoundedButton.Kind.SECONDARY);
        logout.addActionListener(event -> logout());
        header.add(logout, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new javax.swing.BoxLayout(sidebar, javax.swing.BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.CARD);
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 16, 24, 16));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.add(sidebarButton("New Request", RoundedButton.Kind.PRIMARY, () -> new RequestFrame(teacher).setVisible(true)));
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(sidebarButton("My Requests", RoundedButton.Kind.SECONDARY, () -> new MyRequestsFrame(teacher).setVisible(true)));
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(sidebarButton("Chemicals", RoundedButton.Kind.SECONDARY, () -> new ChemicalFrame(teacher, true).setVisible(true)));
        sidebar.add(Box.createVerticalStrut(12));
        return sidebar;
    }

    private JPanel buildContent() {
        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        content.add(buildStats()); content.add(Box.createVerticalStrut(16));
        content.add(buildCharts()); content.add(Box.createVerticalStrut(16));
        content.add(buildRecentCard());
        return content;
    }

    private JPanel buildRecentCard() {
        JTable table = new JTable(requestRows);
        Theme.styleTable(table);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(800, 205)); tableScroll.setMinimumSize(new Dimension(300, 180));
        JPanel recentCard = Theme.card("Recent Requests", "Your latest five requests", tableScroll);
        recentCard.setPreferredSize(new Dimension(900, 285)); recentCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 285));
        recentCard.setAlignmentX(LEFT_ALIGNMENT);
        return recentCard;
    }

    private JPanel buildStats() {
        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(900, 112)); stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        stats.setAlignmentX(LEFT_ALIGNMENT);
        stats.add(Theme.statCard("My Requests", statValues[0], Theme.PRIMARY));
        stats.add(Theme.statCard("Pending", statValues[1], Theme.WARN));
        stats.add(Theme.statCard("Approved", statValues[2], Theme.OK));
        stats.add(Theme.statCard("Rejected", statValues[3], Theme.DANGER));
        return stats;
    }

    private JPanel buildCharts() {
        JPanel charts = new JPanel(new GridLayout(2, 2, 16, 16));
        charts.setOpaque(false);
        charts.setPreferredSize(new Dimension(900, 520)); charts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));
        charts.setMinimumSize(new Dimension(400, 440));
        charts.setAlignmentX(LEFT_ALIGNMENT);
        charts.add(Theme.card("My Requests by Status", "How your requests are progressing", statusChart));
        charts.add(Theme.card("My Requests per Month", "Requests from the last six months", monthChart));
        charts.add(Theme.card("Chemicals I Request Most", "Chemicals included in the most requests", chemicalsChart));
        charts.add(Theme.card("Chemical Availability", "Inventory records by availability status", availabilityChart));
        return charts;
    }

    private RoundedButton sidebarButton(String text, RoundedButton.Kind kind, Runnable action) {
        RoundedButton button = new RoundedButton(text, kind);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.addActionListener(event -> action.run());
        return button;
    }

    private void logout() { dispose(); new LoginFrame().setVisible(true); }

    private void refreshDashboard() {
        if (loading) return;
        loading = true;
        new SwingWorker<TeacherDashboardData, Void>() {
            @Override protected TeacherDashboardData doInBackground() throws Exception {
                return new TeacherDashboardData(requestService.getRequestsForTeacher(teacher.getId()),
                        chemicalService.getAll());
            }

            @Override protected void done() {
                loading = false;
                try {
                    TeacherDashboardData data = get();
                    updateStats(data.requests());
                    updateCharts(data.requests(), data.chemicals());
                    updateRecentRequests(data.requests());
                } catch (Exception exception) {
                    Throwable cause = exception.getCause();
                    String message = cause == null ? exception.getMessage() : cause.getMessage();
                    JOptionPane.showMessageDialog(TeacherDashboard.this,
                            "Unable to load dashboard data. Please check the database connection. - " + message,
                            "Dashboard unavailable", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private record TeacherDashboardData(List<ChemicalRequest> requests, List<Chemical> chemicals) { }

    private void updateStats(List<ChemicalRequest> requests) {
        Map<String, Integer> counts = dashboardService.requestStatusCounts(requests);
        statValues[0].setText(Integer.toString(requests.size()));
        statValues[1].setText(Integer.toString(counts.get(ChemicalRequest.PENDING)));
        statValues[2].setText(Integer.toString(counts.get(ChemicalRequest.APPROVED)));
        statValues[3].setText(Integer.toString(counts.get(ChemicalRequest.REJECTED)));
    }

    private void updateCharts(List<ChemicalRequest> requests, List<Chemical> chemicals) {
        Map<String, Integer> status = dashboardService.requestStatusCounts(requests);
        statusChart.setData(new ArrayList<>(status.keySet()), numbers(status), Arrays.asList(
                Theme.WARN, Theme.OK, Theme.DANGER, Theme.GREY));
        Map<String, Integer> monthly = dashboardService.requestsPerMonth(requests, 6);
        monthChart.setData(requests.isEmpty() ? new ArrayList<>() : new ArrayList<>(monthly.keySet()),
                requests.isEmpty() ? new ArrayList<>() : numbers(monthly), null);
        Map<String, Double> popular = dashboardService.topRequestedChemicals(requests, 5);
        chemicalsChart.setData(new ArrayList<>(popular.keySet()), new ArrayList<>(popular.values()), null);
        Map<String, Integer> availability = dashboardService.inventoryHealth(chemicals);
        availabilityChart.setData(new ArrayList<>(availability.keySet()), numbers(availability), Arrays.asList(
                Theme.OK, Theme.WARN, Theme.WARN, Theme.DANGER));
    }

    private void updateRecentRequests(List<ChemicalRequest> requests) {
        requestRows.setRowCount(0);
        requests.stream().sorted(Comparator.comparing(ChemicalRequest::getRequestDate,
                Comparator.nullsLast(Comparator.reverseOrder()))).limit(5).forEach(request ->
                requestRows.addRow(new Object[] {request.getIdText(), request.getClassName(), request.getExperiment(),
                        request.getStatus(), request.getRequestDate() == null ? "" : request.getRequestDate().format(DATE_FORMAT)}));
    }

    private List<Double> numbers(Map<String, ? extends Number> values) {
        return values.values().stream().map(Number::doubleValue).collect(Collectors.toList());
    }

}
