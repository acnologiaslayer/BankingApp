package com.bank;

import com.bank.exception.DataStoreException;
import com.bank.gui.BankAppGUI;
import com.bank.service.BankManagement;
import com.bank.service.BankOperations;
import com.bank.ui.ConsoleUI;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Entry point: wires the layers together (repository -> service -> UI).
 *
 * Launches the Swing GUI by default. Pass "--console" to use the
 * original terminal interface instead.
 */
public class Main {

    public static void main(String[] args) {
        boolean console = args.length > 0 && args[0].equalsIgnoreCase("--console");
        try {
            BankManagement bankManagement = new BankManagement(Path.of("data"));
            BankOperations bank = new BankOperations(bankManagement);
            if (console) {
                new ConsoleUI(bank, new Scanner(System.in)).run();
            } else {
                SwingUtilities.invokeLater(() -> new BankAppGUI(bank).setVisible(true));
            }
        } catch (DataStoreException e) {
            System.err.println("Could not start the bank: " + e.getMessage());
        }
    }
}
