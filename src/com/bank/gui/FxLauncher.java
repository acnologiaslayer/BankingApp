package com.bank.gui;

import javafx.application.Application;

/**
 * IDE-friendly entry point. Running FxMain directly fails with
 * "JavaFX runtime components are missing" because the Java launcher
 * refuses to start an Application subclass from the classpath.
 * This class does not extend Application, so it works from IntelliJ
 * (and plain java -cp) without any module-path setup.
 */
public class FxLauncher {

    public static void main(String[] args) {
        Application.launch(FxMain.class, args);
    }
}
