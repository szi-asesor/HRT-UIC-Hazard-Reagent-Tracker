package gui.chart;

import gui.Theme;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/** Paints a responsive grouped Java2D bar chart with a series legend. */
public class GroupedBarPanel extends JPanel {
    private static final Color COLOR_A = Theme.PRIMARY;
    private static final Color COLOR_B = Theme.INFO;
    private static final Color TEXT = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private List<String> labels = new ArrayList<>();
    private List<Double> valuesA = new ArrayList<>();
    private List<Double> valuesB = new ArrayList<>();
    private String seriesA = "";
    private String seriesB = "";

    public GroupedBarPanel() {
        setOpaque(true);
        setBackground(Theme.CARD);
        setPreferredSize(new Dimension(320, 220));
        setMinimumSize(new Dimension(160, 120));
    }

    public void setData(List<String> labels, String seriesA, List<Double> a, String seriesB, List<Double> b) {
        int size = labels == null || a == null || b == null
                ? 0 : Math.min(labels.size(), Math.min(a.size(), b.size()));
        this.labels = new ArrayList<>(size);
        valuesA = new ArrayList<>(size);
        valuesB = new ArrayList<>(size);
        this.seriesA = seriesA == null ? "" : seriesA;
        this.seriesB = seriesB == null ? "" : seriesB;
        for (int i = 0; i < size; i++) {
            this.labels.add(labels.get(i) == null ? "" : labels.get(i));
            valuesA.add(number(a.get(i)));
            valuesB.add(number(b.get(i)));
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
        if (labels.isEmpty()) {
            drawEmpty(g);
        } else {
            drawChart(g);
        }
        g.dispose();
    }

    private void drawChart(Graphics2D g) {
        int width = getWidth();
        int height = getHeight();
        FontMetrics metrics = g.getFontMetrics();
        drawLegendItem(g, 10, 8, COLOR_A, truncate(seriesA, metrics, Math.max(30, width / 3)));
        drawLegendItem(g, width / 2, 8, COLOR_B, truncate(seriesB, metrics, Math.max(30, width / 3)));
        int chartTop = 38;
        int chartBottom = Math.max(chartTop + 1, height - 42);
        int chartHeight = chartBottom - chartTop;
        int slot = Math.max(1, width / labels.size());
        int barWidth = Math.max(3, Math.min(20, slot / 3));
        double max = maximum();
        g.setColor(Theme.BORDER);
        g.drawLine(8, chartBottom, width - 8, chartBottom);
        for (int i = 0; i < labels.size(); i++) {
            drawGroup(g, i, width, height, chartBottom, chartHeight, slot, barWidth, max, metrics);
        }
    }

    private void drawGroup(Graphics2D g, int index, int width, int height, int bottom, int chartHeight,
            int slot, int barWidth, double max, FontMetrics metrics) {
        int center = slot * index + slot / 2;
        int firstHeight = barHeight(valuesA.get(index), chartHeight, max);
        int secondHeight = barHeight(valuesB.get(index), chartHeight, max);
        int firstX = center - barWidth - 1;
        int secondX = center + 1;
        drawBar(g, COLOR_A, firstX, bottom, barWidth, firstHeight);
        drawBar(g, COLOR_B, secondX, bottom, barWidth, secondHeight);
        drawValue(g, format(valuesA.get(index)), firstX + barWidth / 2, bottom - firstHeight - 3, width, metrics);
        drawValue(g, format(valuesB.get(index)), secondX + barWidth / 2, bottom - secondHeight - 3, width, metrics);
        String label = truncate(labels.get(index), metrics, Math.max(20, slot - 4));
        int labelX = Math.max(0, Math.min(width - metrics.stringWidth(label),
                center - metrics.stringWidth(label) / 2));
        g.setColor(MUTED);
        g.drawString(label, labelX, height - 10);
    }

    private int barHeight(double value, int chartHeight, double max) {
        return (int) (chartHeight * Math.max(0, value) / max);
    }

    private void drawBar(Graphics2D g, Color color, int x, int bottom, int width, int height) {
        if (height <= 0) return;
        g.setColor(color);
        g.fillRoundRect(x, bottom - height, width, height, 6, 6);
    }

    private void drawLegendItem(Graphics2D g, int x, int y, Color color, String label) {
        g.setColor(color);
        g.fillRoundRect(x, y, 10, 10, 4, 4);
        g.setColor(TEXT);
        g.drawString(label, x + 15, y + 10);
    }

    private void drawValue(Graphics2D g, String value, int center, int y, int width, FontMetrics metrics) {
        int x = Math.max(0, Math.min(width - metrics.stringWidth(value), center - metrics.stringWidth(value) / 2));
        g.setColor(TEXT);
        g.drawString(value, x, Math.max(metrics.getAscent(), y));
    }

    private void drawEmpty(Graphics2D g) {
        String text = "No data yet";
        g.setColor(MUTED);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
                (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2);
    }

    private double maximum() {
        double max = 0;
        for (int i = 0; i < valuesA.size(); i++) {
            max = Math.max(max, Math.max(valuesA.get(i), valuesB.get(i)));
        }
        return max == 0 ? 1 : max;
    }

    private static double number(Double value) {
        return value == null || !Double.isFinite(value) ? 0 : value;
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
