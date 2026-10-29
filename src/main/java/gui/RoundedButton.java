package gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;

/** A readable pill-shaped button with primary, secondary, and danger styles. */
public class RoundedButton extends JButton {
    private static final long serialVersionUID = 1L;

    public enum Kind { PRIMARY, SECONDARY, DANGER }

    private static final Color DISABLED_BACKGROUND = new Color(0xE5E7EB);
    private static final Color DISABLED_TEXT = new Color(0x6B7280);
    private final Kind kind;
    private boolean hovered;

    @SuppressWarnings("this-escape")
    public RoundedButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setFont(Theme.BODY_BOLD);
        setForeground(textColor());
        configureAppearance();
        configureSize(text);
        trackHover();
        getAccessibleContext().setAccessibleName(text);
    }

    private void configureAppearance() {
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void configureSize(String text) {
        setMinimumSize(new Dimension(120, 42));
        FontMetrics metrics = getFontMetrics(getFont());
        setPreferredSize(new Dimension(Math.max(120, metrics.stringWidth(text) + 48), 42));
    }

    private void trackHover() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintButton(g);
        g.dispose();
    }

    private void paintButton(Graphics2D g) {
        int inset = hasFocus() ? 3 : 1;
        Color background = backgroundColor();
        Color foreground = isEnabled() ? textColor() : DISABLED_TEXT;
        if (hasFocus() && isEnabled()) {
            g.setColor(Theme.PRIMARY_DARK);
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1,
                    getHeight(), getHeight()));
        }
        g.setColor(background);
        g.fill(new RoundRectangle2D.Float(inset, inset, getWidth() - inset * 2,
                getHeight() - inset * 2, getHeight(), getHeight()));
        if (kind == Kind.SECONDARY && isEnabled()) {
            g.setColor(hovered ? Theme.PRIMARY_DARK : Theme.PRIMARY);
            g.draw(new RoundRectangle2D.Float(inset + 0.5f, inset + 0.5f,
                    getWidth() - inset * 2 - 1, getHeight() - inset * 2 - 1,
                    getHeight(), getHeight()));
        }
        paintLabel(g, foreground);
    }

    private void paintLabel(Graphics2D g, Color foreground) {
        g.setFont(getFont());
        g.setColor(foreground);
        FontMetrics metrics = g.getFontMetrics();
        String text = getText();
        int textX = (getWidth() - metrics.stringWidth(text)) / 2;
        int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(text, textX, textY);
    }

    private Color backgroundColor() {
        if (!isEnabled()) {
            return DISABLED_BACKGROUND;
        }
        if (kind == Kind.SECONDARY) {
            return hovered || getModel().isPressed() ? new Color(0xD5EEEA) : Color.WHITE;
        }
        Color base = kind == Kind.DANGER ? Theme.DANGER : Theme.PRIMARY;
        if (kind == Kind.PRIMARY && (hovered || getModel().isPressed())) {
            return Theme.PRIMARY_DARK;
        }
        return hovered || getModel().isPressed() ? base.darker() : base;
    }

    private Color textColor() {
        if (kind == Kind.SECONDARY) {
            return isEnabled() ? Theme.PRIMARY : DISABLED_TEXT;
        }
        return isEnabled() ? Color.WHITE : DISABLED_TEXT;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        setForeground(enabled ? textColor() : DISABLED_TEXT);
        repaint();
    }
}
