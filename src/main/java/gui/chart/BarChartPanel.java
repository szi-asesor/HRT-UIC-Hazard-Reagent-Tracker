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

/** Paints a responsive Java2D bar chart. */
public class BarChartPanel extends JPanel {
    private static final Color PRIMARY = Theme.PRIMARY;
    private static final Color TEXT = Theme.TEXT;
    private static final Color MUTED = Theme.MUTED;
    private final boolean horizontal;
    private List<String> labels = new ArrayList<>();
    private List<Double> values = new ArrayList<>();
    private List<Color> colors = new ArrayList<>();
    private List<String> suffixes = new ArrayList<>();

    public BarChartPanel(boolean horizontal) {
        this.horizontal = horizontal;
        setOpaque(true);
        setBackground(Theme.CARD);
        setPreferredSize(new Dimension(320, 220));
        setMinimumSize(new Dimension(160, 120));
    }

    public void setData(List<String> labels, List<Double> values, List<Color> colors) {
        setData(labels, values, colors, null);
    }

    /** Sets data with a per-value unit suffix, useful when bars use different units. */
    public void setData(List<String> labels, List<Double> values, List<Color> colors, List<String> suffixes) {
        int size = labels == null || values == null ? 0 : Math.min(labels.size(), values.size());
        this.labels = new ArrayList<>(size);
        this.values = new ArrayList<>(size);
        this.colors = new ArrayList<>(size);
        this.suffixes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.labels.add(labels.get(i) == null ? "" : labels.get(i));
            Double value = values.get(i);
            this.values.add(value == null || !Double.isFinite(value) ? 0.0 : value);
            this.colors.add(colors == null || i >= colors.size() || colors.get(i) == null ? PRIMARY : colors.get(i));
            this.suffixes.add(suffixes == null || i >= suffixes.size() || suffixes.get(i) == null
                    ? "" : " " + suffixes.get(i));
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
        } else if (horizontal) {
            drawHorizontal(g);
        } else {
            drawVertical(g);
        }
        g.dispose();
    }

    private void drawHorizontal(Graphics2D g) {
        int width = getWidth();
        int height = getHeight();
        int labelWidth = Math.min(130, Math.max(55, width / 3));
        int valueWidth = 48;
        int left = labelWidth + 8;
        int available = Math.max(1, width - left - valueWidth - 12);
        int rowHeight = Math.max(18, (height - 16) / labels.size());
        double max = maximum();
        FontMetrics metrics = g.getFontMetrics();
        for (int i = 0; i < labels.size(); i++) {
            int y = 8 + i * rowHeight;
            String label = truncate(labels.get(i), metrics, labelWidth - 4);
            g.setColor(TEXT);
            g.drawString(label, 4, y + (rowHeight + metrics.getAscent() - metrics.getDescent()) / 2);
            int barWidth = (int) (available * Math.max(0, values.get(i)) / max);
            int barY = y + Math.max(2, (rowHeight - 14) / 2);
            if (values.get(i) > 0) {
                g.setColor(colors.get(i));
                g.fillRoundRect(left, barY, Math.max(2, barWidth), 14, 8, 8);
            }
            g.setColor(TEXT);
            g.drawString(format(values.get(i)) + suffixes.get(i), left + available + 6,
                    y + (rowHeight + metrics.getAscent() - metrics.getDescent()) / 2);
        }
    }

    private void drawVertical(Graphics2D g) {
        int width = getWidth();
        int height = getHeight();
        int count = labels.size();
        int chartTop = 18;
        int chartBottom = Math.max(chartTop + 1, height - 48);
        int chartHeight = chartBottom - chartTop;
        int slot = Math.max(1, width / count);
        int barWidth = Math.max(4, Math.min(34, slot * 2 / 3));
        double max = maximum();
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(Theme.BORDER);
        g.drawLine(8, chartBottom, width - 8, chartBottom);
        for (int i = 0; i < count; i++) {
            drawVerticalBar(g, i, width, height, chartBottom, chartHeight, slot, barWidth, max, metrics);
        }
    }

    private void drawVerticalBar(Graphics2D g, int index, int width, int height, int bottom, int chartHeight,
            int slot, int barWidth, double max, FontMetrics metrics) {
        int center = slot * index + slot / 2;
        double value = values.get(index);
        int barHeight = (int) (chartHeight * Math.max(0, value) / max);
        if (barHeight > 0) {
            g.setColor(colors.get(index));
            g.fillRoundRect(center - barWidth / 2, bottom - barHeight, barWidth, barHeight, 8, 8);
        }
        g.setColor(TEXT);
        String text = format(value);
        int textX = center - metrics.stringWidth(text) / 2;
        g.drawString(text, Math.max(0, Math.min(width - metrics.stringWidth(text), textX)),
                Math.max(metrics.getAscent(), bottom - barHeight - 4));
        String label = truncate(labels.get(index), metrics, Math.max(20, slot - 4));
        int labelX = center - metrics.stringWidth(label) / 2;
        g.setColor(MUTED);
        g.drawString(label, Math.max(0, Math.min(width - metrics.stringWidth(label), labelX)), height - 12);
    }

    private void drawEmpty(Graphics2D g) {
        String text = "No data yet";
        g.setFont(Theme.SMALL);
        g.setColor(MUTED);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
                (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2);
    }

    private double maximum() {
        double max = 0;
        for (double value : values) {
            max = Math.max(max, value);
        }
        return max == 0 ? 1 : max;
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
