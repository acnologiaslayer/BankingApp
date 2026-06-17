package com.bank.gui;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;

/**
 * The themes offered in the Theme menu. Each one maps to a Swing
 * Look&Feel; the dark and CYBERPUNK variants additionally override
 * UIManager colour keys (and, for Cyberpunk, the button UI) to restyle
 * the whole interface.
 */
public enum SwingTheme {

    SYSTEM("System"),
    METAL("Metal"),
    NIMBUS("Nimbus"),
    NIMBUS_DARK("Nimbus Dark"),
    CYBERPUNK("Cyberpunk");

    /** Nimbus colour keys touched by the dark/cyberpunk variants. */
    private static final String[] NIMBUS_OVERRIDE_KEYS = {
            "control", "info", "nimbusBase", "nimbusLightBackground",
            "nimbusFocus", "nimbusSelectionBackground", "nimbusSelection",
            "text", "background", "nimbusBlueGrey", "menu", "menuText",
            "textForeground", "Table.background", "Table.foreground",
            "Table.alternateRowColor", "TableHeader.background",
            "TableHeader.foreground", "Panel.background", "Label.foreground",
            "MenuBar.background", "Menu.foreground", "MenuItem.background",
            "MenuItem.foreground", "ScrollPane.background", "Viewport.background",
            "TitledBorder.titleColor"
    };

    /** Cyberpunk neon palette. */
    private static final Color CP_BG = new Color(10, 10, 24);
    private static final Color CP_PANEL = new Color(16, 16, 34);
    private static final Color CP_CYAN = new Color(0, 240, 255);
    private static final Color CP_PINK = new Color(255, 42, 109);
    private static final Color CP_TEXT = new Color(224, 248, 255);

    private final String label;

    SwingTheme(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static SwingTheme fromLabel(String label) {
        for (SwingTheme theme : values()) {
            if (theme.label.equals(label)) {
                return theme;
            }
        }
        return SYSTEM;
    }

    /** Installs this theme's Look&Feel and decorations into the UIManager. */
    public void apply() throws Exception {
        clearOverrides();
        switch (this) {
            case SYSTEM -> UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            case METAL -> UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            case NIMBUS -> UIManager.setLookAndFeel(nimbusClassName());
            case NIMBUS_DARK -> {
                installNimbusDarkColors();
                UIManager.setLookAndFeel(nimbusClassName());
            }
            case CYBERPUNK -> {
                installCyberpunkColors();
                UIManager.setLookAndFeel(nimbusClassName());
            }
        }
    }

    private static String nimbusClassName() throws Exception {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                return info.getClassName();
            }
        }
        throw new Exception("Nimbus Look&Feel is not available on this JVM.");
    }

    private static void installNimbusDarkColors() {
        UIManager.put("control", new Color(60, 63, 65));
        UIManager.put("info", new Color(60, 63, 65));
        UIManager.put("nimbusBase", new Color(18, 30, 49));
        UIManager.put("nimbusLightBackground", new Color(43, 43, 43));
        UIManager.put("nimbusFocus", new Color(115, 164, 209));
        UIManager.put("nimbusSelectionBackground", new Color(82, 109, 165));
        UIManager.put("text", new Color(230, 230, 230));
    }

    private static void installCyberpunkColors() {
        // Base Nimbus palette: deep midnight backgrounds, neon highlights.
        UIManager.put("control", CP_PANEL);
        UIManager.put("info", CP_PANEL);
        UIManager.put("nimbusBase", new Color(28, 12, 48));
        UIManager.put("nimbusBlueGrey", new Color(24, 24, 48));
        UIManager.put("nimbusLightBackground", CP_BG);
        UIManager.put("background", CP_BG);
        UIManager.put("nimbusFocus", CP_CYAN);
        UIManager.put("nimbusSelection", CP_PINK);
        UIManager.put("nimbusSelectionBackground", new Color(80, 16, 60));
        UIManager.put("text", CP_TEXT);
        UIManager.put("menu", CP_PANEL);
        UIManager.put("menuText", CP_CYAN);
        UIManager.put("textForeground", CP_TEXT);

        // Component-level keys so the table and panels match.
        UIManager.put("Panel.background", new ColorUIResource(CP_BG));
        UIManager.put("Label.foreground", new ColorUIResource(CP_CYAN));
        UIManager.put("Table.background", new ColorUIResource(CP_BG));
        UIManager.put("Table.foreground", new ColorUIResource(CP_TEXT));
        UIManager.put("Table.alternateRowColor", new ColorUIResource(CP_PANEL));
        UIManager.put("Table.selectionBackground", new ColorUIResource(new Color(80, 16, 60)));
        UIManager.put("Table.selectionForeground", new ColorUIResource(CP_CYAN));
        UIManager.put("TableHeader.background", new ColorUIResource(new Color(28, 12, 48)));
        UIManager.put("TableHeader.foreground", new ColorUIResource(CP_PINK));

        // Menu bar, scroll pane and titled borders.
        UIManager.put("MenuBar.background", new ColorUIResource(new Color(28, 12, 48)));
        UIManager.put("Menu.foreground", new ColorUIResource(CP_CYAN));
        UIManager.put("MenuItem.background", new ColorUIResource(CP_PANEL));
        UIManager.put("MenuItem.foreground", new ColorUIResource(CP_CYAN));
        UIManager.put("ScrollPane.background", new ColorUIResource(CP_BG));
        UIManager.put("Viewport.background", new ColorUIResource(CP_BG));
        UIManager.put("TitledBorder.titleColor", new ColorUIResource(CP_PINK));

        // The custom angular neon button UI.
        UIManager.put("ButtonUI", CyberpunkButtonUI.class.getName());
    }

    private static void clearOverrides() {
        for (String key : NIMBUS_OVERRIDE_KEYS) {
            UIManager.put(key, null);
        }
        // Reset the component keys touched by Cyberpunk.
        for (String key : new String[]{
                "Table.selectionBackground", "Table.selectionForeground",
                "TableHeader.background", "TableHeader.foreground"}) {
            UIManager.put(key, null);
        }
        // Restore the default button UI (removes the custom cyberpunk buttons).
        UIManager.put("ButtonUI", null);
    }

    /** True for themes whose custom decorations are pure-Java (always safe). */
    public boolean isCustom() {
        return this == CYBERPUNK;
    }
}
