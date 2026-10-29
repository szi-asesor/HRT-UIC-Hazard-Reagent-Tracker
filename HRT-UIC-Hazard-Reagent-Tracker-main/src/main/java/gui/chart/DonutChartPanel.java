package gui.chart;

import gui.Theme;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.List;

/** Paints a responsive Java2D donut chart with a labeled legend. */
public class DonutChartPanel extends JPanel {
    private static final Color[] DEFAULT_COLORS = {
            Theme.OK, Theme.WARN, Theme.INFO, Theme.DANGER, Theme.GREY
    };
    private static final Color TEXT = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private List<String> labels = new ArrayList<>();
    private List<Double> values = new ArrayList<>();
    private List<Color> colors = new ArrayList<>();

    public DonutChartPanel() {
        setOpaque(true);
        setBackground(Theme.CARD);
        setPreferredSize(new Dimension(320, 220));
        setMinimumSize(new Dimension(160, 120));
    }

    public void setData(List<String> labels, List<Double> values, List<Color> colors) {
        int size = labels == null || values == null ? 0 : Math.min(labels.size(), values.size());
        this.labels = new ArrayList<>(size);
        this.values = new ArrayList<>(size);
        this.colors = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.labels.add(labels.get(i) == null ? "" : labels.get(i));
            Double value = values.get(i);
            this.values.add(value == null || !Double.isFinite(value) ? 0.0 : Math.max(0, value));
            Color color = colors == null || i >= colors.size() ? null : colors.get(i);
            this.colors.add(color == null ? DEFAULT_COLORS[i % DEFAULT_COLORS.length] : color);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(Theme.SMALL);
        if (labels.isEmpty() || total() <= 0) {
            drawEmpty(g);
        } else {
            drawChart(g);
        }
        g.dispose();
    }

    private void drawChart(Graphics2D g) {
        int size = Math.max(24, Math.min(getHeight() - 24, getWidth() - 204));
        int x = 8;
        int y = (getHeight() - size) / 2;
        double total = total();
        double start = 90;
        for (int i = 0; i < values.size(); i++) {
            double extent = 360 * values.get(i) / total;
            if (extent > 0) {
                g.setColor(colors.get(i));
                g.fill(new Arc2D.Double(x, y, size, size, start, extent, Arc2D.PIE));
                start += extent;
            }
        }
        int hole = size / 2;
        g.setColor(Theme.CARD);
        g.fillOval(x + (size - hole) / 2, y + (size - hole) / 2, hole, hole);
        String totalText = format(total);
        g.setFont(Theme.TABLE.deriveFont(Font.BOLD));
        FontMetrics totalMetrics = g.getFontMetrics();
        g.setColor(TEXT);
        g.drawString(totalText, x + (size - totalMetrics.stringWidth(totalText)) / 2,
                y + (size + totalMetrics.getAscent() - totalMetrics.getDescent()) / 2);
        drawLegend(g, x + size + 28, total);
    }

    private void drawLegend(Graphics2D g, int x, double total) {
        g.setFont(Theme.SMALL);
        FontMetrics metrics = g.getFontMetrics();
        int available = Math.max(24, getWidth() - x - 6);
        int rowHeight = Math.max(18, Math.min(24, (getHeight() - 8) / labels.size()));
        for (int i = 0; i < labels.size(); i++) {
            int y = 6 + i * rowHeight;
            g.setColor(colors.get(i));
            g.fillRoundRect(x, y + 3, 10, 10, 4, 4);
            String text = truncate(labels.get(i) + " " + format(values.get(i))
                    + " (" + Math.round(values.get(i) * 100 / total) + "%)", metrics, available - 16);
            g.setColor(TEXT);
            g.drawString(text, x + 15, y + metrics.getAscent() + 2);
        }
    }

    private void drawEmpty(Graphics2D g) {
        String text = "No data yet";
        g.setColor(MUTED);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
                (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2);
    }

    private double total() {
        double total = 0;
        for (double value : values) {
            total += value;
        }
        return total;
    }

    private static String format(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format("%.1f", value);
    }

    private static String truncate(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return end == 0 ? ellipsis : text.substring(0, end) + ellipsis;
    }
}
