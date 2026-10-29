package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/** Shared colors, typography, and Swing styling helpers. */
public final class Theme {
    public static final Color BG = new Color(0xF4F7FA);
    public static final Color CARD = new Color(0xFFFFFF);
    public static final Color PRIMARY = new Color(0x0F766E);
    public static final Color PRIMARY_DARK = new Color(0x115E59);
    public static final Color TEXT = new Color(0x1F2937);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color BORDER = new Color(0xE5E7EB);
    public static final Color OK = new Color(0x16A34A);
    public static final Color WARN = new Color(0xD97706);
    public static final Color DANGER = new Color(0xB91C1C);
    public static final Color INFO = new Color(0x2563EB);
    public static final Color GREY = new Color(0x9CA3AF);

    private static final String FONT_FAMILY = chooseFontFamily();
    public static final Font TITLE = new Font(FONT_FAMILY, Font.BOLD, 28);
    public static final Font SECTION = new Font(FONT_FAMILY, Font.BOLD, 18);
    public static final Font BODY = new Font(FONT_FAMILY, Font.PLAIN, 15);
    public static final Font BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 15);
    public static final Font TABLE = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font SMALL = new Font(FONT_FAMILY, Font.PLAIN, 13);

    private Theme() {
    }

    public static void install() {
        UIManager.put("Label.font", BODY); UIManager.put("Button.font", BODY_BOLD);
        UIManager.put("TextField.font", BODY); UIManager.put("PasswordField.font", BODY);
        UIManager.put("TextArea.font", BODY); UIManager.put("ComboBox.font", BODY);
        UIManager.put("CheckBox.font", BODY); UIManager.put("RadioButton.font", BODY);
        UIManager.put("Table.font", TABLE);
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("TableHeader.font", TABLE.deriveFont(Font.BOLD));
        UIManager.put("OptionPane.messageFont", BODY); UIManager.put("OptionPane.buttonFont", BODY_BOLD);
        UIManager.put("Panel.background", BG); UIManager.put("OptionPane.background", BG);
    }

    public static JPanel card(String title, String caption, JComponent content) {
        JPanel card = new RoundedPanel();
        card.setLayout(new java.awt.BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel heading = new JPanel(new java.awt.BorderLayout(0, 4));
        heading.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SECTION); titleLabel.setForeground(TEXT);
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(BODY); captionLabel.setForeground(MUTED);
        heading.add(titleLabel, java.awt.BorderLayout.NORTH);
        heading.add(captionLabel, java.awt.BorderLayout.CENTER);
        card.add(heading, java.awt.BorderLayout.NORTH);
        card.add(content, java.awt.BorderLayout.CENTER);
        return card;
    }

    public static JPanel statCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new RoundedPanel();
        card.setLayout(new java.awt.BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(BODY); labelComponent.setForeground(MUTED);
        valueLabel.setFont(TITLE); valueLabel.setForeground(TEXT);
        card.add(labelComponent, java.awt.BorderLayout.NORTH);
        card.add(valueLabel, java.awt.BorderLayout.CENTER);
        return card;
    }

    public static void styleTable(JTable table) {
        table.setFont(TABLE); table.setRowHeight(32);
        table.setShowGrid(false); table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false); table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 1)); table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(PRIMARY); table.setSelectionForeground(Color.WHITE);
        table.setBackground(CARD);
        table.setForeground(TEXT);
        styleTableRows(table);
        styleTableHeader(table);
        table.setBorder(BorderFactory.createLineBorder(BORDER));
        if (table.getParent() instanceof JComponent) {
            ((JComponent) table.getParent()).setBackground(CARD);
        }
    }

    private static void styleTableRows(JTable table) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focused, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                setFont(TABLE);
                setForeground(selected ? Color.WHITE : TEXT);
                setBackground(selected ? PRIMARY : (row % 2 == 0 ? CARD : new Color(0xF9FAFB)));
                return this;
            }
        });
    }

    private static void styleTableHeader(JTable table) {
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setFont(TABLE.deriveFont(Font.BOLD)); header.setForeground(TEXT);
            header.setBackground(new Color(0xF3F4F6));
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
            header.setReorderingAllowed(false);
        }
    }

    public static void styleInput(JComponent field) {
        field.setFont(BODY); field.setForeground(TEXT); field.setBackground(CARD);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 38));
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedInputBorder(), BorderFactory.createEmptyBorder(0, 8, 0, 8)));
    }

    public static Dimension screenPercent(double pct) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        double scale = Math.max(0.1, Math.min(1.0, pct));
        return new Dimension((int) Math.round(screen.width * scale),
                (int) Math.round(screen.height * scale));
    }

    private static String chooseFontFamily() {
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if ("Segoe UI".equalsIgnoreCase(family)) {
                return family;
            }
        }
        return Font.SANS_SERIF;
    }

    private static final class RoundedPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private RoundedPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(CARD);
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
            g.setColor(BORDER);
            g.setStroke(new BasicStroke(1));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class RoundedInputBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(BORDER);
            g.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, 10, 10));
            g.dispose();
        }
    }
}
