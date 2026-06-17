package com.bank.gui;

import com.bank.exception.BankException;
import com.bank.exception.DataStoreException;
import com.bank.model.Account;
import com.bank.service.BankManagement;
import com.bank.service.BankOperations;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.List;

/**
 * Java Swing front-end for the banking application.
 *
 * Built in the same style as the Playground DivisionEngineGUI lesson:
 *  - extends JFrame,
 *  - builds the layout in the constructor,
 *  - wires buttons with anonymous ActionListeners,
 *  - keeps all business logic in the service layer (BankOperations).
 */
public class BankAppGUI extends JFrame {

    private final BankOperations bank;

    // form fields shared by the action panels
    private JComboBox<String> typeField;
    private JTextField nameField;
    private JTextField amountField;
    private JTextField accountField;
    private JTextField targetAccountField;

    private JTable accountTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    public BankAppGUI(BankOperations bank) {
        this.bank = bank;

        setTitle("Amar Bank - Banking Application");
        setSize(820, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildFormPanel(), BorderLayout.WEST);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        refreshTable();
    }

    // ---------- layout builders ----------

    private JComponent buildHeader() {
        JLabel title = new JLabel("AMAR BANK", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        return title;
    }

    private JComponent buildFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        typeField = new JComboBox<>(new String[]{"SAVINGS", "CURRENT"});
        nameField = new JTextField(14);
        amountField = new JTextField(14);
        accountField = new JTextField(14);
        targetAccountField = new JTextField(14);

        panel.add(labelled("Account type:", typeField));
        panel.add(labelled("Customer name:", nameField));
        panel.add(labelled("Account number:", accountField));
        panel.add(labelled("Target account (transfer):", targetAccountField));
        panel.add(labelled("Amount / opening balance:", amountField));
        panel.add(Box.createVerticalStrut(10));

        panel.add(actionButton("Open Account", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openAccount();
            }
        }));
        panel.add(actionButton("Deposit", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deposit();
            }
        }));
        panel.add(actionButton("Withdraw", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                withdraw();
            }
        }));
        panel.add(actionButton("Transfer", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                transfer();
            }
        }));
        panel.add(actionButton("Check Balance", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkBalance();
            }
        }));
        panel.add(actionButton("Refresh List", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshTable();
                setStatus("Account list refreshed.");
            }
        }));

        return panel;
    }

    private JComponent buildTablePanel() {
        String[] columns = {"Account No", "Type", "Customer", "Balance", "Special Attribute"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only display
            }
        };
        accountTable = new JTable(tableModel);
        accountTable.setRowHeight(24);
        accountTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(accountTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Accounts"));
        return scrollPane;
    }

    private JComponent buildStatusBar() {
        statusLabel = new JLabel("Ready.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return statusLabel;
    }

    // ---------- small UI helpers ----------

    private JPanel labelled(String text, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        row.add(new JLabel(text), BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return row;
    }

    private JButton actionButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        button.addActionListener(listener);
        return button;
    }

    // ---------- actions (delegate to the service layer) ----------

    private void openAccount() {
        try {
            String type = (String) typeField.getSelectedItem();
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                throw new BankException("Customer name cannot be empty.");
            }
            double opening = parseAmount(amountField.getText());
            Account account = bank.openAccount(type, name, opening);
            refreshTable();
            clearInputs();
            setStatus("Account created: " + account.getAccountNumber());
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void deposit() {
        try {
            String number = requireAccountNumber();
            double amount = parseAmount(amountField.getText());
            bank.deposit(number, amount);
            refreshTable();
            setStatus(String.format("Deposited %.2f. New balance: %.2f",
                    amount, bank.requireAccount(number).getBalance()));
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void withdraw() {
        try {
            String number = requireAccountNumber();
            double amount = parseAmount(amountField.getText());
            bank.withdraw(number, amount);
            refreshTable();
            setStatus(String.format("Withdrew %.2f. New balance: %.2f",
                    amount, bank.requireAccount(number).getBalance()));
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void transfer() {
        try {
            String from = requireAccountNumber();
            String to = targetAccountField.getText().trim();
            if (to.isEmpty()) {
                throw new BankException("Target account number is required for a transfer.");
            }
            double amount = parseAmount(amountField.getText());
            bank.transfer(from, to, amount);
            refreshTable();
            setStatus(String.format("Transferred %.2f from %s to %s.", amount, from, to));
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void checkBalance() {
        try {
            String number = requireAccountNumber();
            Account account = bank.requireAccount(number);
            setStatus(String.format("%s (%s): balance %.2f, withdrawable %.2f",
                    account.getAccountNumber(), account.getType(),
                    account.getBalance(), account.withdrawableBalance()));
        } catch (BankException e) {
            showError(e.getMessage());
        }
    }

    // ---------- shared helpers ----------

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Account> accounts = bank.listAccounts();
        for (Account account : accounts) {
            tableModel.addRow(new Object[]{
                    account.getAccountNumber(),
                    account.getType(),
                    account.getAccountHolderName(),
                    String.format("%.2f", account.getBalance()),
                    describeSpecialAttribute(account)
            });
        }
    }

    private String describeSpecialAttribute(Account account) {
        return account.getType().equals("SAVINGS")
                ? "Interest 5.00%"
                : String.format("Overdraft %.2f", account.withdrawableBalance() - account.getBalance());
    }

    private String requireAccountNumber() throws BankException {
        String number = accountField.getText().trim();
        if (number.isEmpty()) {
            throw new BankException("Please enter an account number.");
        }
        return number;
    }

    private double parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new NumberFormatException("Please enter an amount.");
        }
        return Double.parseDouble(text.trim());
    }

    private void clearInputs() {
        nameField.setText("");
        amountField.setText("");
    }

    private void setStatus(String message) {
        statusLabel.setForeground(new Color(0, 110, 0));
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText("Error: " + message);
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ---------- entry point ----------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                BankManagement bankManagement = new BankManagement(Path.of("data"));
                BankOperations bank = new BankOperations(bankManagement);
                new BankAppGUI(bank).setVisible(true);
            } catch (DataStoreException e) {
                JOptionPane.showMessageDialog(null,
                        "Could not start the bank: " + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
