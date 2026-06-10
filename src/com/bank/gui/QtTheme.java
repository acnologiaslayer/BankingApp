package com.bank.gui;

import io.qt.widgets.QApplication;

/**
 * The themes offered in the Theme menu. SYSTEM is the platform default;
 * the others apply a Qt stylesheet (QSS) to the whole application,
 * which automatically covers every window and dialog.
 */
public enum QtTheme {

    SYSTEM("System", ""),

    DARK("Dark", """
            QWidget { background-color: #2b2e33; color: #e6e6e6; }
            QLineEdit, QComboBox, QTableWidget { background-color: #34383d; color: #e6e6e6; }
            QHeaderView::section { background-color: #3b3f44; color: #e6e6e6; padding: 4px; border: 0; }
            QTableWidget { gridline-color: #4a4f55; selection-background-color: #4d78cc; }
            QTableCornerButton::section { background-color: #3b3f44; }
            QPushButton { background-color: #3b3f44; border: 1px solid #555a60; padding: 5px 14px; border-radius: 3px; }
            QPushButton:hover { background-color: #4a4f55; }
            QPushButton:pressed { background-color: #2f3338; }
            QMenuBar { background-color: #3b3f44; }
            QMenuBar::item:selected { background-color: #4a4f55; }
            QMenu { background-color: #34383d; color: #e6e6e6; }
            QMenu::item:selected { background-color: #4d78cc; }
            """),

    OCEAN("Ocean", """
            QWidget { background-color: #e8f1f6; color: #1d3947; }
            QLineEdit, QComboBox, QTableWidget { background-color: #ffffff; }
            QHeaderView::section { background-color: #d7e5ee; padding: 4px; border: 0; }
            QTableWidget { selection-background-color: #1b6d96; selection-color: white; }
            QPushButton { background-color: #2e86ab; color: white; border: none; padding: 5px 14px; border-radius: 3px; }
            QPushButton:hover { background-color: #1b6d96; }
            QPushButton:pressed { background-color: #14516f; }
            QMenuBar { background-color: #d7e5ee; }
            QMenu { background-color: #ffffff; }
            QMenu::item:selected { background-color: #1b6d96; color: white; }
            """);

    private final String label;
    private final String styleSheet;

    QtTheme(String label, String styleSheet) {
        this.label = label;
        this.styleSheet = styleSheet;
    }

    public String getLabel() {
        return label;
    }

    public static QtTheme fromLabel(String label) {
        for (QtTheme theme : values()) {
            if (theme.label.equals(label)) {
                return theme;
            }
        }
        return SYSTEM;
    }

    /** Applies this theme's stylesheet application-wide. */
    public void apply() {
        QApplication.instance().setStyleSheet(styleSheet);
    }
}
