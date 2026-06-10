package com.bank.gui;

import com.bank.exception.BankException;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.service.BankService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main window: a table of accounts (column names visible) with a
 * toolbar of banking actions. All business rules stay in BankService;
 * this class only collects input and shows results.
 */
public class BankFrame extends JFrame {

    private static final String[] ACCOUNT_COLUMNS =
            {"Account No", "Type", "Customer", "Phone", "Balance"};
    private static final String[] TRANSACTION_COLUMNS =
            {"Date & Time", "Type", "Amount", "Balance After"};
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BankService bank;
    private final DefaultTableModel accountsModel;
    private final JTable accountsTable;

    public BankFrame(BankService bank) {
        super("Banking Application");
        this.bank = bank;

        accountsModel = new DefaultTableModel(ACCOUNT_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // balances change only through the service
            }
        };
        accountsTable = new JTable(accountsModel);
        accountsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setLayout(new BorderLayout(8, 8));
        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(accountsTable), BorderLayout.CENTER);

        refreshAccounts();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 420);
        setLocationRelativeTo(null);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(actionButton("Open Account", this::openAccount));
        toolbar.add(actionButton("Deposit", this::deposit));
        toolbar.add(actionButton("Withdraw", this::withdraw));
        toolbar.add(actionButton("Transfer", this::transfer));
        toolbar.add(actionButton("History", this::showHistory));
        toolbar.add(actionButton("Apply Interest", this::applyInterest));
        return toolbar;
    }

    private JButton actionButton(String label, BankAction action) {
        JButton button = new JButton(label);
        button.addActionListener(e -> {
            try {
                action.run();
            } catch (BankException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Operation failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        return button;
    }

    /** Like Runnable, but allowed to throw domain exceptions. */
    @FunctionalInterface
    private interface BankAction {
        void run() throws BankException;
    }

    // ---------- actions ----------

    private void openAccount() throws BankException {
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"SAVINGS", "CURRENT"});
        JTextField nameField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField balanceField = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Account type:"));
        form.add(typeBox);
        form.add(new JLabel("Customer name:"));
        form.add(nameField);
        form.add(new JLabel("Phone:"));
        form.add(phoneField);
        form.add(new JLabel("Opening balance:"));
        form.add(balanceField);

        int result = JOptionPane.showConfirmDialog(this, form, "Open Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        Account account = bank.openAccount(
                (String) typeBox.getSelectedItem(),
                nameField.getText().trim(),
                phoneField.getText().trim(),
                parseAmount(balanceField.getText()));
        refreshAccounts();
        JOptionPane.showMessageDialog(this,
                "Account created: " + account.getAccountNumber());
    }

    private void deposit() throws BankException {
        String number = selectedAccountNumber();
        double amount = promptAmount("Amount to deposit into " + number + ":");
        bank.deposit(number, amount);
        refreshAccounts();
    }

    private void withdraw() throws BankException {
        String number = selectedAccountNumber();
        double amount = promptAmount("Amount to withdraw from " + number + ":");
        bank.withdraw(number, amount);
        refreshAccounts();
    }

    private void transfer() throws BankException {
        String from = selectedAccountNumber();
        String to = JOptionPane.showInputDialog(this, "Transfer from " + from + " to account:");
        if (to == null) {
            return;
        }
        double amount = promptAmount("Amount to transfer:");
        bank.transfer(from, to.trim(), amount);
        refreshAccounts();
    }

    private void showHistory() throws BankException {
        String number = selectedAccountNumber();
        List<Transaction> history = bank.historyOf(number);

        DefaultTableModel model = new DefaultTableModel(TRANSACTION_COLUMNS, 0);
        for (Transaction t : history) {
            model.addRow(new Object[]{
                    t.getTimestamp().format(TIMESTAMP_FORMAT),
                    t.getType(),
                    String.format("%.2f", t.getAmount()),
                    String.format("%.2f", t.getBalanceAfter())
            });
        }

        JDialog dialog = new JDialog(this, "History of " + number, true);
        JScrollPane pane = new JScrollPane(new JTable(model));
        pane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        dialog.add(pane);
        dialog.setSize(560, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void applyInterest() throws BankException {
        double total = bank.applyInterestToSavings();
        refreshAccounts();
        JOptionPane.showMessageDialog(this,
                String.format("Interest credited to all savings accounts: %.2f", total));
    }

    // ---------- helpers ----------

    private void refreshAccounts() {
        accountsModel.setRowCount(0);
        for (Account account : bank.listAccounts()) {
            accountsModel.addRow(new Object[]{
                    account.getAccountNumber(),
                    account.getType(),
                    account.getOwner().getName(),
                    account.getOwner().getPhone(),
                    String.format("%.2f", account.getBalance())
            });
        }
    }

    private String selectedAccountNumber() throws BankException {
        int row = accountsTable.getSelectedRow();
        if (row < 0) {
            throw new BankException("Select an account in the table first.");
        }
        return (String) accountsModel.getValueAt(row, 0);
    }

    private double promptAmount(String message) throws BankException {
        String input = JOptionPane.showInputDialog(this, message);
        if (input == null) {
            throw new BankException("Operation cancelled.");
        }
        return parseAmount(input);
    }

    private double parseAmount(String input) throws BankException {
        try {
            return Double.parseDouble(input.trim());
        } catch (NumberFormatException e) {
            throw new BankException("Not a valid number: " + input);
        }
    }
}
