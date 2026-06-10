package com.bank.gui;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;
import java.util.List;

/**
 * The themes offered in the Theme menu. LIGHT is the JavaFX default
 * (Modena); the others load a CSS stylesheet from the resources.
 */
public enum FxTheme {

    LIGHT("Light", null),
    DARK("Dark", "/themes/dark.css"),
    OCEAN("Ocean", "/themes/ocean.css");

    private final String label;
    private final String stylesheet;

    FxTheme(String label, String stylesheet) {
        this.label = label;
        this.stylesheet = stylesheet;
    }

    public String getLabel() {
        return label;
    }

    public static FxTheme fromLabel(String label) {
        for (FxTheme theme : values()) {
            if (theme.label.equals(label)) {
                return theme;
            }
        }
        return LIGHT;
    }

    /** The stylesheet URLs this theme needs (empty for the default look). */
    public List<String> stylesheetUrls() {
        if (stylesheet == null) {
            return List.of();
        }
        URL url = FxTheme.class.getResource(stylesheet);
        return url == null ? List.of() : List.of(url.toExternalForm());
    }

    public void apply(Scene scene) {
        scene.getStylesheets().setAll(stylesheetUrls());
    }

    /** Dialogs have their own scene, so they must be themed separately. */
    public void apply(DialogPane pane) {
        pane.getStylesheets().setAll(stylesheetUrls());
    }
}
