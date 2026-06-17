package com.bank.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * A custom, fully theme-able window title bar used by the undecorated
 * {@link BankAppGUI} frame. Because the frame is undecorated, this bar
 * (and the window border) is painted by us, so the whole window matches
 * the active {@link SwingTheme} instead of the native OS chrome.
 *
 * Provides: the app title, drag-to-move, double-click to maximise, and
 * minimise / maximise / close buttons.
 */
public class TitleBar extends JPanel {

    private final JFrame frame;
    private final JLabel titleLabel;
    private final ChromeButton minimiseButton;
    private final ChromeButton maximiseButton;
    private final ChromeButton closeButton;

    private Point dragOffset;
    private Rectangle normalBounds; // remembered bounds before maximising

    public TitleBar(JFrame frame, String title) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 6));
        setPreferredSize(new Dimension(10, 40));

        titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(titleLabel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 6));
        buttons.setOpaque(false);
        minimiseButton = new ChromeButton(ChromeButton.Glyph.MINIMISE);
        maximiseButton = new ChromeButton(ChromeButton.Glyph.MAXIMISE);
        closeButton = new ChromeButton(ChromeButton.Glyph.CLOSE);
        minimiseButton.addActionListener(e -> frame.setExtendedState(Frame.ICONIFIED));
        maximiseButton.addActionListener(e -> toggleMaximise());
        closeButton.addActionListener(e -> frame.dispatchEvent(
                new java.awt.event.WindowEvent(frame, java.awt.event.WindowEvent.WINDOW_CLOSING)));
        buttons.add(minimiseButton);
        buttons.add(maximiseButton);
        buttons.add(closeButton);
        add(buttons, BorderLayout.EAST);

        installDragSupport();
    }

    /** Recolours the bar and its buttons for the given palette. */
    public void applyPalette(SwingTheme.Palette palette) {
        setBackground(palette.surface());
        titleLabel.setForeground(palette.accent());
        minimiseButton.applyPalette(palette, false);
        maximiseButton.applyPalette(palette, false);
        closeButton.applyPalette(palette, true);
        repaint();
    }

    private void toggleMaximise() {
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        Rectangle screen = gc.getBounds();
        Insets si = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        Rectangle usable = new Rectangle(screen.x + si.left, screen.y + si.top,
                screen.width - si.left - si.right, screen.height - si.top - si.bottom);

        if (normalBounds == null) {
            normalBounds = frame.getBounds();
            frame.setBounds(usable);
        } else {
            frame.setBounds(normalBounds);
            normalBounds = null;
        }
        frame.revalidate();
    }

    private void installDragSupport() {
        MouseAdapter press = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragOffset = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    toggleMaximise();
                }
            }
        };
        MouseMotionAdapter drag = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset == null || normalBounds != null) {
                    return; // not dragging, or currently maximised
                }
                Point onScreen = e.getLocationOnScreen();
                frame.setLocation(onScreen.x - dragOffset.x, onScreen.y - dragOffset.y);
            }
        };
        addMouseListener(press);
        addMouseMotionListener(drag);
        titleLabel.addMouseListener(press);
        titleLabel.addMouseMotionListener(drag);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // A thin accent underline gives the bar a HUD-like edge.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(titleLabel.getForeground());
        g2.fillRect(0, getHeight() - 2, getWidth(), 2);
        g2.dispose();
    }
}
