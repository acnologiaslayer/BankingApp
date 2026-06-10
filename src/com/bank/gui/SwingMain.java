package com.bank.gui;

import com.bank.exception.DataStoreException;
import com.bank.repository.BankRepository;
import com.bank.repository.FileBankRepository;
import com.bank.service.BankService;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.nio.file.Path;

/**
 * Swing entry point: wires the same repository and service used by
 * the console UI, then opens the main window.
 */
public class SwingMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                SwingTheme.fromLabel(UiSettings.loadTheme("System")).apply();
            } catch (Exception ignored) {
                // fall back to the JVM default Look&Feel
            }
            try {
                BankRepository repository = new FileBankRepository(Path.of("data"));
                BankService bank = new BankService(repository);
                new BankFrame(bank).setVisible(true);
            } catch (DataStoreException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(),
                        "Could not start the bank", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
