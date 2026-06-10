package com.bank.gui;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * File-based UI preferences (currently just the selected theme),
 * stored next to the bank data in data/ui-settings.properties.
 */
public final class UiSettings {

    private static final Path FILE = Path.of("data", "ui-settings.properties");
    private static final String THEME_KEY = "theme";

    private UiSettings() {
    }

    /** Returns the saved theme name, or the fallback if none was saved. */
    public static String loadTheme(String fallback) {
        try {
            if (Files.exists(FILE)) {
                Properties props = new Properties();
                props.load(Files.newBufferedReader(FILE));
                return props.getProperty(THEME_KEY, fallback);
            }
        } catch (IOException ignored) {
            // a missing or unreadable settings file just means "use the default"
        }
        return fallback;
    }

    public static void saveTheme(String theme) {
        try {
            Files.createDirectories(FILE.getParent());
            Properties props = new Properties();
            props.setProperty(THEME_KEY, theme);
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                props.store(writer, "BankingApp UI settings");
            }
        } catch (IOException ignored) {
            // losing a theme preference is not worth interrupting the user
        }
    }
}
