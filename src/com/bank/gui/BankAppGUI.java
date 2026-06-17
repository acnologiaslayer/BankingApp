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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
 *
 * Theme switching is fault-tolerant: some Look&Feels (notably the native
 * GTK "System" L&F) can throw asynchronously during painting on certain
 * JVMs. A {@link SafeEventQueue} catches those uncaught event-thread
 * errors so the application reverts to the previous working theme and
 * shows a clear message instead of crashing.
 */
public class BankAppGUI extends JFrame {

    private static final String[] ACCOUNT_TYPES = {"SAVINGS", "CURRENT"};

    /** Pure-Java theme used at startup and as the recovery fallback. */
    private static final SwingTheme DEFAULT_THEME = SwingTheme.NIMBUS;

    private final BankOperations bank;
    private final Map<SwingTheme, JRadioButtonMenuItem> themeItems = new EnumMap<>(SwingTheme.class);

    private JTable accountTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    // theme state used for error recovery
    private SwingTheme currentTheme = DEFAULT_THEME;
    private SwingTheme lastGoodTheme = DEFAULT_THEME;
    private boolean recoveringTheme = false;
    private Timer promoteTimer;

    public BankAppGUI(BankOperations bank) {
        this.bank = bank;

        installDefaultTheme();

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

        // Catch any uncaught event-thread error (e.g. a Look&Feel that fails
        // to paint) and route it through our recovery/reporting handler.
        SafeEventQueue.install(this::handleEventThreadError);
    }

    // ---------- layout builders ----------

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu themeMenu = new JMenu("Theme");
        ButtonGroup group = new ButtonGroup();
        for (SwingTheme theme : SwingTheme.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.getLabel());
            item.setSelected(theme == currentTheme);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    applyTheme(theme);
                }
            });
            group.add(item);
            themeMenu.add(item);
            themeItems.put(theme, item);
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

    // ---------- theme handling ----------

    /** Applies the default theme at startup, with a hard fallback chain. */
    private void installDefaultTheme() {
        try {
            DEFAULT_THEME.apply();
            currentTheme = DEFAULT_THEME;
            lastGoodTheme = DEFAULT_THEME;
        } catch (Exception e) {
            // Fall back to the cross-platform L&F, which is always available.
            try {
                SwingTheme.METAL.apply();
                currentTheme = SwingTheme.METAL;
                lastGoodTheme = SwingTheme.METAL;
            } catch (Exception ignored) {
                // Keep whatever L&F the JVM started with.
            }
        }
    }

    /**
     * Switches to the requested theme. If applying it fails immediately we
     * revert here; if it fails later during painting, {@link #handleEventThreadError}
     * performs the recovery. Either way the user sees a clear message.
     */
    private void applyTheme(SwingTheme theme) {
        if (theme == currentTheme) {
            return;
        }
        SwingTheme previousGood = lastGoodTheme;
        try {
            theme.apply();
            currentTheme = theme;
            selectThemeItem(theme);
            SwingUtilities.updateComponentTreeUI(this);
            // Only trust the new theme once it has survived a short settling
            // period of real repaints. A broken Look&Feel throws during those
            // repaints first, so lastGoodTheme is not advanced prematurely.
            scheduleThemePromotion(theme);
            setStatus("Theme changed to " + theme.getLabel() + ".");
        } catch (Throwable e) {
            revertTheme(previousGood, theme, describeThrowable(e));
        }
    }

    /** After a brief delay with no paint failures, accept the theme as safe. */
    private void scheduleThemePromotion(SwingTheme theme) {
        if (promoteTimer != null) {
            promoteTimer.stop();
        }
        promoteTimer = new Timer(400, e -> markThemeAsGood(theme));
        promoteTimer.setRepeats(false);
        promoteTimer.start();
    }

    /** Called after a successful repaint to remember a theme as safe. */
    private void markThemeAsGood(SwingTheme theme) {
        if (currentTheme == theme && !recoveringTheme) {
            lastGoodTheme = theme;
        }
    }

    /**
     * Handles any exception that escaped to the event thread. A failure that
     * comes from a Look&Feel (painting) triggers a revert to the last working
     * theme; anything else is reported as an unexpected error.
     */
    private void handleEventThreadError(Throwable error) {
        if (recoveringTheme) {
            return; // already recovering; swallow follow-up paint errors
        }
        if (isLookAndFeelError(error) && currentTheme != lastGoodTheme) {
            recoveringTheme = true;
            if (promoteTimer != null) {
                promoteTimer.stop(); // never promote the broken theme
            }
            SwingTheme broken = currentTheme;
            try {
                revertTheme(lastGoodTheme, broken, describeThrowable(error));
            } finally {
                recoveringTheme = false;
            }
        } else if (!isLookAndFeelError(error)) {
            showError("An unexpected error occurred:\n" + describeThrowable(error));
        }
        // A look&feel error while we are already on the last-good theme is
        // a transient repaint glitch from the failed switch; safe to ignore.
    }

    /** Restores a known-good theme and tells the user what went wrong. */
    private void revertTheme(SwingTheme target, SwingTheme broken, String reason) {
        SwingTheme restored = target;
        try {
            target.apply();
            currentTheme = target;
        } catch (Throwable e) {
            // Last resort: the cross-platform L&F never fails to load.
            try {
                SwingTheme.METAL.apply();
                restored = SwingTheme.METAL;
                currentTheme = SwingTheme.METAL;
            } catch (Throwable ignored) {
                restored = currentTheme;
            }
        }
        lastGoodTheme = currentTheme;
        selectThemeItem(currentTheme);
        try {
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Throwable ignored) {
            // updating the tree should be safe now; ignore if not
        }
        statusLabel.setForeground(Color.RED);
        statusLabel.setText("Theme '" + broken.getLabel() + "' is not supported here; reverted to "
                + restored.getLabel() + ".");
        // Show the dialog after we have fully unwound from the failing paint
        // dispatch, so we never start a nested modal loop inside the handler.
        final String restoredLabel = restored.getLabel();
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                "The '" + broken.getLabel() + "' theme is not supported on this system "
                        + "and could not be displayed.\n\nReverted to the '" + restoredLabel
                        + "' theme.\n\nDetails: " + reason,
                "Theme Not Available", JOptionPane.WARNING_MESSAGE));
    }

    private void selectThemeItem(SwingTheme theme) {
        JRadioButtonMenuItem item = themeItems.get(theme);
        if (item != null) {
            item.setSelected(true);
        }
    }

    private boolean isLookAndFeelError(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            for (StackTraceElement frame : t.getStackTrace()) {
                String cls = frame.getClassName();
                if (cls.contains("javax.swing.plaf") || cls.contains("com.sun.java.swing.plaf")) {
                    return true;
                }
            }
        }
        return false;
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
        if (ensureAccountsExist()) {
            return;
        }
        FormDialog form = new FormDialog(this, "Deposit");
        JComboBox<String> account = form.addComboBox("Account:", accountChoices());
        JTextField amount = form.addTextField("Amount to deposit:");
        preselectAccount(account);
        if (!form.showDialog()) {
            return;
        }
        try {
            String number = accountNumberOf(account);
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
        if (ensureAccountsExist()) {
            return;
        }
        FormDialog form = new FormDialog(this, "Withdraw");
        JComboBox<String> account = form.addComboBox("Account:", accountChoices());
        JTextField amount = form.addTextField("Amount to withdraw:");
        preselectAccount(account);
        if (!form.showDialog()) {
            return;
        }
        try {
            String number = accountNumberOf(account);
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
        if (bank.listAccounts().size() < 2) {
            showError("You need at least two accounts to make a transfer.");
            return;
        }
        FormDialog form = new FormDialog(this, "Transfer");
        JComboBox<String> from = form.addComboBox("From account:", accountChoices());
        JComboBox<String> to = form.addComboBox("To account:", accountChoices());
        JTextField amount = form.addTextField("Amount to transfer:");
        preselectAccount(from);
        if (!form.showDialog()) {
            return;
        }
        try {
            String fromNumber = accountNumberOf(from);
            String toNumber = accountNumberOf(to);
            double value = parseAmount(amount.getText());
            bank.transfer(fromNumber, toNumber, value);
            refreshTable();
            setStatus(String.format("Transferred %.2f from %s to %s.", value, fromNumber, toNumber));
        } catch (BankException | NumberFormatException e) {
            showError(e.getMessage());
        }
    }

    private void checkBalance() {
        if (ensureAccountsExist()) {
            return;
        }
        FormDialog form = new FormDialog(this, "Check Balance");
        JComboBox<String> account = form.addComboBox("Account:", accountChoices());
        preselectAccount(account);
        if (!form.showDialog()) {
            return;
        }
        try {
            String number = accountNumberOf(account);
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

    /** Builds the labelled choices ("AC00001 - Alice (SAVINGS)") for an account dropdown. */
    private String[] accountChoices() {
        List<Account> accounts = bank.listAccounts();
        String[] choices = new String[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) {
            Account a = accounts.get(i);
            choices[i] = String.format("%s - %s (%s)",
                    a.getAccountNumber(), a.getAccountHolderName(), a.getType());
        }
        return choices;
    }

    /** Extracts the account number from a "AC00001 - Name (TYPE)" dropdown item. */
    private String accountNumberOf(JComboBox<String> combo) {
        Object selected = combo.getSelectedItem();
        String text = selected == null ? "" : selected.toString();
        int dash = text.indexOf(" - ");
        return (dash >= 0 ? text.substring(0, dash) : text).trim();
    }

    /** Selects the table-highlighted account in the dropdown, if one is highlighted. */
    private void preselectAccount(JComboBox<String> combo) {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        String number = String.valueOf(tableModel.getValueAt(row, 0));
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).startsWith(number + " ")) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Warns and returns true when there are no accounts to act on yet. */
    private boolean ensureAccountsExist() {
        if (bank.listAccounts().isEmpty()) {
            showError("There are no accounts yet. Please open an account first.");
            return true;
        }
        return false;
    }

    private double parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new NumberFormatException("Please enter a valid amount.");
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("'" + text.trim() + "' is not a valid number.");
        }
    }

    private String describeThrowable(Throwable t) {
        String message = t.getMessage();
        if (message == null || message.isBlank()) {
            message = t.getClass().getSimpleName();
        }
        return message;
    }

    private void setStatus(String message) {
        statusLabel.setForeground(new Color(0, 110, 0));
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText("Error: " + message.replace('\n', ' '));
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
            } catch (Throwable t) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                JOptionPane.showMessageDialog(null,
                        "The application failed to start:\n" + t,
                        "Fatal Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
