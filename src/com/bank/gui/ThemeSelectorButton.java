package com.bank.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * The theme selector shown in the bottom-right of the window. Instead of a
 * "Theme" menu it is a small custom-painted palette icon; clicking it pops
 * up the list of available themes. The icon recolours to match the active
 * theme palette.
 */
public class ThemeSelectorButton extends JButton {

    private final JPopupMenu popup = new JPopupMenu();
    private final ButtonGroup group = new ButtonGroup();
    private SwingTheme.Palette palette = SwingTheme.NIMBUS.palette();
    private boolean hovering;

    public ThemeSelectorButton(SwingTheme current, Consumer<SwingTheme> onSelect) {
        setPreferredSize(new Dimension(34, 34));
        setToolTipText("Change theme");
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        for (SwingTheme theme : SwingTheme.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.getLabel());
            item.setSelected(theme == current);
            item.addActionListener(e -> onSelect.accept(theme));
            group.add(item);
            popup.add(item);
        }

        addActionListener(e -> popup.show(this,
                getWidth() - popup.getPreferredSize().width, -popup.getPreferredSize().height - 4));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                repaint();
            }
        });
    }

    /** Marks the given theme as selected in the popup. */
    public void setSelected(SwingTheme theme) {
        int i = 0;
        for (java.awt.Component c : popup.getComponents()) {
            if (c instanceof JRadioButtonMenuItem item) {
                item.setSelected(SwingTheme.values()[i].getLabel().equals(theme.getLabel()));
                i++;
            }
        }
    }

    public void applyPalette(SwingTheme.Palette palette) {
        this.palette = palette;
        popup.setBackground(palette.surface());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (hovering) {
            g2.setColor(new Color(palette.accent().getRed(), palette.accent().getGreen(),
                    palette.accent().getBlue(), 50));
            g2.fillRoundRect(1, 1, w - 2, h - 2, 10, 10);
        }

        // A painter's-palette glyph: a rounded blob with paint dots, drawn
        // in the theme accent colour with multi-coloured dots.
        int cx = w / 2;
        int cy = h / 2;
        int r = Math.min(w, h) / 2 - 6;

        g2.setColor(palette.accent());
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);

        Color[] dots = {palette.accent(), palette.danger(), palette.foreground()};
        int dotR = Math.max(2, r / 4);
        int[][] offsets = {{-r / 2, -r / 3}, {r / 2, -r / 3}, {0, r / 2}};
        for (int i = 0; i < dots.length; i++) {
            g2.setColor(dots[i]);
            g2.fillOval(cx + offsets[i][0] - dotR / 2, cy + offsets[i][1] - dotR / 2, dotR, dotR);
        }
        g2.dispose();
    }
}
