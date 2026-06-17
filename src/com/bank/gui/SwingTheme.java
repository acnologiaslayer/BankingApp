package com.bank.gui;

import javax.swing.UIManager;
import java.awt.Color;

/**
 * The themes offered in the Theme menu. Each one maps to a Swing
 * Look&Feel; NIMBUS_DARK additionally overrides Nimbus colour keys
 * to produce a dark variant.
 */
public enum SwingTheme {

    SYSTEM("System"),
    METAL("Metal"),
    NIMBUS("Nimbus"),
    NIMBUS_DARK("Nimbus Dark");

    private static final String[] NIMBUS_DARK_KEYS = {
            "control", "info", "nimbusBase", "nimbusLightBackground",
            "nimbusFocus", "nimbusSelectionBackground", "text"
    };

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

    /** Installs this theme's Look&Feel into the UIManager. */
    public void apply() throws Exception {
        clearNimbusOverrides();
        switch (this) {
            case SYSTEM -> UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            case METAL -> UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            case NIMBUS -> UIManager.setLookAndFeel(nimbusClassName());
            case NIMBUS_DARK -> {
                installNimbusDarkColors();
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

    private static void clearNimbusOverrides() {
        for (String key : NIMBUS_DARK_KEYS) {
            UIManager.put(key, null);
        }
    }
}
