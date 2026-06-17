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
 *
 * Each action opens its own modal pop-up form (FormDialog). A Theme menu
 * lets the user switch the Swing Look&Feel at runtime.
 */
public class BankAppGUI extends JFrame {

    private static final String[] ACCOUNT_TYPES = {"SAVINGS", "CURRENT"};

    private final BankOperations bank;

    private JTable accountTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    public BankAppGUI(BankOperations bank) {
        this.bank = bank;

        setTitle("Amar Bank - Banking Application");
        setSize(760, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        setJMenuBar(buildMenuBar());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildToolbar(), BorderLayout.WEST);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        refreshTable();
    }

    // ---------- layout builders ----------

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu themeMenu = new JMenu("Theme");
        ButtonGroup group = new ButtonGroup();
        for (SwingTheme theme : SwingTheme.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.getLabel());
            if (theme == SwingTheme.SYSTEM) {
                item.setSelected(true);
            }
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    applyTheme(theme);
                }
            });
            group.add(item);
            themeMenu.add(item);
        }
        menuBar.add(themeMenu);
        return menuBar;
    }

    private JComponent buildHeader() {
        JLabel title = new JLabel("AMAR BANK", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        return title;
    }

    private JComponent buildToolbar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        panel.add(actionButton("Open Account...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openAccount();
            }
        }));
        panel.add(actionButton("Deposit...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deposit();
            }
        }));
        panel.add(actionButton("Withdraw...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                withdraw();
            }
        }));
        panel.add(actionButton("Transfer...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                transfer();
            }
        }));
        panel.add(actionButton("Check Balance...", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkBalance();
            }
        }));
        panel.add(Box.createVerticalStrut(10));
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

    private JButton actionButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.addActionListener(listener);
        return button;
    }

    // ---------- theme ----------

    private void applyTheme(SwingTheme theme) {
        try {
            theme.apply();
            SwingUtilities.updateComponentTreeUI(this);
            pack();
            setSize(760, 520);
            setStatus("Theme changed to " + theme.getLabel() + ".");
        } catch (Exception e) {
            showError("Could not apply theme: " + e.getMessage());
        }
    }

    // ---------- actions (each opens a pop-up form) ----------

    private void openAccount() {
        FormDialog form = new FormDialog(this, "Open Account");
        JComboBox<String> type = form.addComboBox("Account type:", ACCOUNT_TYPES);
        JTextField name = form.addTextField("Customer name:");
        JTextField opening = form.addTextField("Opening balance:");
        if (!form.showDialog()) {
            return;
        }
        try {
            String customer = name.getText().trim();
            if (customer.isEmpty()) {
                throw new BankException("Customer name cannot be empty.");
            }
            double balance = parseAmount(opening.getText());
            Account account = bank.openAccount((String) type.getSelectedItem(), customer, balance);
            refreshTable();
            setStatus("Account created: " + account.getAccountNumber());
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void deposit() {
        FormDialog form = new FormDialog(this, "Deposit");
        JTextField account = form.addTextField("Account number:");
        JTextField amount = form.addTextField("Amount to deposit:");
        prefillSelectedAccount(account);
        if (!form.showDialog()) {
            return;
        }
        try {
            String number = requireText(account, "Please enter an account number.");
            double value = parseAmount(amount.getText());
            bank.deposit(number, value);
            refreshTable();
            setStatus(String.format("Deposited %.2f. New balance: %.2f",
                    value, bank.requireAccount(number).getBalance()));
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void withdraw() {
        FormDialog form = new FormDialog(this, "Withdraw");
        JTextField account = form.addTextField("Account number:");
        JTextField amount = form.addTextField("Amount to withdraw:");
        prefillSelectedAccount(account);
        if (!form.showDialog()) {
            return;
        }
        try {
            String number = requireText(account, "Please enter an account number.");
            double value = parseAmount(amount.getText());
            bank.withdraw(number, value);
            refreshTable();
            setStatus(String.format("Withdrew %.2f. New balance: %.2f",
                    value, bank.requireAccount(number).getBalance()));
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void transfer() {
        FormDialog form = new FormDialog(this, "Transfer");
        JTextField from = form.addTextField("From account:");
        JTextField to = form.addTextField("To account:");
        JTextField amount = form.addTextField("Amount to transfer:");
        prefillSelectedAccount(from);
        if (!form.showDialog()) {
            return;
        }
        try {
            String fromNumber = requireText(from, "Please enter the source account.");
            String toNumber = requireText(to, "Please enter the target account.");
            double value = parseAmount(amount.getText());
            bank.transfer(fromNumber, toNumber, value);
            refreshTable();
            setStatus(String.format("Transferred %.2f from %s to %s.", value, fromNumber, toNumber));
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void checkBalance() {
        FormDialog form = new FormDialog(this, "Check Balance");
        JTextField account = form.addTextField("Account number:");
        prefillSelectedAccount(account);
        if (!form.showDialog()) {
            return;
        }
        try {
            String number = requireText(account, "Please enter an account number.");
            Account acc = bank.requireAccount(number);
            String message = String.format(
                    "Account: %s%nType: %s%nCustomer: %s%nBalance: %.2f%nWithdrawable now: %.2f",
                    acc.getAccountNumber(), acc.getType(), acc.getAccountHolderName(),
                    acc.getBalance(), acc.withdrawableBalance());
            JOptionPane.showMessageDialog(this, message, "Balance", JOptionPane.INFORMATION_MESSAGE);
            setStatus(String.format("%s balance: %.2f", acc.getAccountNumber(), acc.getBalance()));
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

    /** Pre-fills a field with the account number selected in the table, if any. */
    private void prefillSelectedAccount(JTextField field) {
        int row = accountTable.getSelectedRow();
        if (row >= 0) {
            field.setText(String.valueOf(tableModel.getValueAt(row, 0)));
        }
    }

    private String requireText(JTextField field, String error) throws BankException {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new BankException(error);
        }
        return value;
    }

    private double parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new NumberFormatException("Please enter an amount.");
        }
        return Double.parseDouble(text.trim());
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
