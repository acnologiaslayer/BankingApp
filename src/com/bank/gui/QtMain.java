package com.bank.gui;

import com.bank.exception.BankException;
import com.bank.exception.DataStoreException;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.repository.FileBankRepository;
import com.bank.service.BankService;

import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QApplication;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QDialog;
import io.qt.widgets.QDialogButtonBox;
import io.qt.widgets.QFormLayout;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QMainWindow;
import io.qt.widgets.QMessageBox;
import io.qt.widgets.QPushButton;
import io.qt.widgets.QTableWidget;
import io.qt.widgets.QTableWidgetItem;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Qt front end, built with Qt Jambi (the Java bindings for Qt 6).
 * Same layering as the console, Swing and JavaFX UIs: this class only
 * collects input and renders tables; every rule lives in BankService.
 */
public class QtMain extends QMainWindow {

    private static final List<String> ACCOUNT_COLUMNS =
            List.of("Account No", "Type", "Customer", "Phone", "Balance");
    private static final List<String> TRANSACTION_COLUMNS =
            List.of("Date & Time", "Type", "Amount", "Balance After");
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BankService bank;
    private final QTableWidget accountsTable = new QTableWidget();

    public static void main(String[] args) {
        QApplication.initialize(args);
        try {
            BankService bank = new BankService(new FileBankRepository(Path.of("data")));
            QtMain window = new QtMain(bank);
            window.show();
            QApplication.exec();
        } catch (DataStoreException e) {
            QMessageBox.critical(null, "Could not start the bank", e.getMessage());
        } finally {
            QApplication.shutdown();
        }
    }

    public QtMain(BankService bank) {
        this.bank = bank;
        setWindowTitle("Banking Application");
        resize(780, 430);

        accountsTable.setColumnCount(ACCOUNT_COLUMNS.size());
        accountsTable.setHorizontalHeaderLabels(ACCOUNT_COLUMNS);
        accountsTable.setEditTriggers(QAbstractItemView.EditTrigger.NoEditTriggers);
        accountsTable.setSelectionBehavior(QAbstractItemView.SelectionBehavior.SelectRows);
        accountsTable.setSelectionMode(QAbstractItemView.SelectionMode.SingleSelection);

        QHBoxLayout buttons = new QHBoxLayout();
        buttons.addWidget(actionButton("Open Account", this::openAccount));
        buttons.addWidget(actionButton("Deposit", this::deposit));
        buttons.addWidget(actionButton("Withdraw", this::withdraw));
        buttons.addWidget(actionButton("Transfer", this::transfer));
        buttons.addWidget(actionButton("History", this::showHistory));
        buttons.addWidget(actionButton("Apply Interest", this::applyInterest));
        buttons.addStretch();

        QWidget central = new QWidget();
        QVBoxLayout root = new QVBoxLayout(central);
        root.addLayout(buttons);
        root.addWidget(accountsTable);
        setCentralWidget(central);

        refreshAccounts();
    }

    private QPushButton actionButton(String label, BankAction action) {
        QPushButton button = new QPushButton(label);
        button.clicked.connect(() -> {
            try {
                action.run();
            } catch (BankException ex) {
                QMessageBox.critical(this, "Operation failed", ex.getMessage());
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
        QDialog dialog = new QDialog(this);
        dialog.setWindowTitle("Open Account");

        QComboBox typeBox = new QComboBox();
        typeBox.addItems(List.of("SAVINGS", "CURRENT"));
        QLineEdit nameField = new QLineEdit();
        QLineEdit phoneField = new QLineEdit();
        QLineEdit balanceField = new QLineEdit();

        QDialogButtonBox buttonBox = new QDialogButtonBox(
                QDialogButtonBox.StandardButton.Ok.combined(QDialogButtonBox.StandardButton.Cancel));
        buttonBox.accepted.connect(dialog::accept);
        buttonBox.rejected.connect(dialog::reject);

        QFormLayout form = new QFormLayout(dialog);
        form.addRow("Account type:", typeBox);
        form.addRow("Customer name:", nameField);
        form.addRow("Phone:", phoneField);
        form.addRow("Opening balance:", balanceField);
        form.addRow(buttonBox);

        if (dialog.exec() != QDialog.DialogCode.Accepted.value()) {
            return;
        }

        Account account = bank.openAccount(
                typeBox.currentText(),
                nameField.text().trim(),
                phoneField.text().trim(),
                parseAmount(balanceField.text()));
        refreshAccounts();
        QMessageBox.information(this, "Account created",
                "Account created: " + account.getAccountNumber());
    }

    private void deposit() throws BankException {
        String number = selectedAccountNumber();
        bank.deposit(number, promptAmount("Amount to deposit into " + number + ":"));
        refreshAccounts();
    }

    private void withdraw() throws BankException {
        String number = selectedAccountNumber();
        bank.withdraw(number, promptAmount("Amount to withdraw from " + number + ":"));
        refreshAccounts();
    }

    private void transfer() throws BankException {
        String from = selectedAccountNumber();
        String to = promptText("Transfer from " + from + " to account:");
        bank.transfer(from, to, promptAmount("Amount to transfer:"));
        refreshAccounts();
    }

    private void showHistory() throws BankException {
        String number = selectedAccountNumber();
        List<Transaction> history = bank.historyOf(number);

        QTableWidget table = new QTableWidget();
        table.setColumnCount(TRANSACTION_COLUMNS.size());
        table.setHorizontalHeaderLabels(TRANSACTION_COLUMNS);
        table.setEditTriggers(QAbstractItemView.EditTrigger.NoEditTriggers);
        table.setRowCount(history.size());
        for (int row = 0; row < history.size(); row++) {
            Transaction t = history.get(row);
            table.setItem(row, 0, new QTableWidgetItem(t.getTimestamp().format(TIMESTAMP_FORMAT)));
            table.setItem(row, 1, new QTableWidgetItem(t.getType().name()));
            table.setItem(row, 2, new QTableWidgetItem(String.format("%.2f", t.getAmount())));
            table.setItem(row, 3, new QTableWidgetItem(String.format("%.2f", t.getBalanceAfter())));
        }

        QDialog dialog = new QDialog(this);
        dialog.setWindowTitle("History of " + number);
        dialog.resize(580, 340);
        QVBoxLayout layout = new QVBoxLayout(dialog);
        layout.addWidget(table);
        dialog.exec();
    }

    private void applyInterest() throws BankException {
        double total = bank.applyInterestToSavings();
        refreshAccounts();
        QMessageBox.information(this, "Interest applied",
                String.format("Interest credited to all savings accounts: %.2f", total));
    }

    // ---------- helpers ----------

    private void refreshAccounts() {
        List<Account> accounts = bank.listAccounts();
        accountsTable.setRowCount(accounts.size());
        for (int row = 0; row < accounts.size(); row++) {
            Account account = accounts.get(row);
            accountsTable.setItem(row, 0, new QTableWidgetItem(account.getAccountNumber()));
            accountsTable.setItem(row, 1, new QTableWidgetItem(account.getType()));
            accountsTable.setItem(row, 2, new QTableWidgetItem(account.getOwner().getName()));
            accountsTable.setItem(row, 3, new QTableWidgetItem(account.getOwner().getPhone()));
            accountsTable.setItem(row, 4, new QTableWidgetItem(String.format("%.2f", account.getBalance())));
        }
    }

    private String selectedAccountNumber() throws BankException {
        int row = accountsTable.currentRow();
        if (row < 0) {
            throw new BankException("Select an account in the table first.");
        }
        return accountsTable.item(row, 0).text();
    }

    private String promptText(String message) throws BankException {
        QDialog dialog = new QDialog(this);
        dialog.setWindowTitle("Input");
        QLineEdit field = new QLineEdit();
        QDialogButtonBox buttonBox = new QDialogButtonBox(
                QDialogButtonBox.StandardButton.Ok.combined(QDialogButtonBox.StandardButton.Cancel));
        buttonBox.accepted.connect(dialog::accept);
        buttonBox.rejected.connect(dialog::reject);
        QFormLayout form = new QFormLayout(dialog);
        form.addRow(message, field);
        form.addRow(buttonBox);
        if (dialog.exec() != QDialog.DialogCode.Accepted.value()) {
            throw new BankException("Operation cancelled.");
        }
        return field.text().trim();
    }

    private double promptAmount(String message) throws BankException {
        return parseAmount(promptText(message));
    }

    private double parseAmount(String input) throws BankException {
        try {
            return Double.parseDouble(input.trim());
        } catch (NumberFormatException e) {
            throw new BankException("Not a valid number: " + input);
        }
    }
}
