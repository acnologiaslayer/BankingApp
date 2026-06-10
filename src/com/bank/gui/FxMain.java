package com.bank.gui;

import com.bank.exception.BankException;
import com.bank.exception.DataStoreException;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.repository.FileBankRepository;
import com.bank.service.BankService;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * JavaFX front end. Same layering as the console and Swing UIs:
 * this class only collects input and renders tables; every rule
 * lives in BankService.
 */
public class FxMain extends Application {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private BankService bank;
    private final TableView<Account> accountsTable = new TableView<>();
    private FxTheme theme = FxTheme.LIGHT;
    private Scene scene;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            bank = new BankService(new FileBankRepository(Path.of("data")));
        } catch (DataStoreException e) {
            showError("Could not start the bank: " + e.getMessage());
            return;
        }

        buildAccountColumns();
        refreshAccounts();

        BorderPane root = new BorderPane();
        root.setTop(new VBox(buildMenuBar(), buildToolbar()));
        root.setCenter(accountsTable);

        theme = FxTheme.fromLabel(UiSettings.loadTheme("Light"));
        scene = new Scene(root, 780, 430);
        theme.apply(scene);

        stage.setTitle("Banking Application");
        stage.setScene(scene);
        stage.show();
    }

    private MenuBar buildMenuBar() {
        Menu themeMenu = new Menu("Theme");
        ToggleGroup group = new ToggleGroup();
        String saved = UiSettings.loadTheme("Light");

        for (FxTheme candidate : FxTheme.values()) {
            RadioMenuItem item = new RadioMenuItem(candidate.getLabel());
            item.setToggleGroup(group);
            item.setSelected(candidate.getLabel().equals(saved));
            item.setOnAction(e -> {
                theme = candidate;
                theme.apply(scene);
                UiSettings.saveTheme(theme.getLabel());
            });
            themeMenu.getItems().add(item);
        }
        return new MenuBar(themeMenu);
    }

    private void buildAccountColumns() {
        TableColumn<Account, String> number = new TableColumn<>("Account No");
        number.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getAccountNumber()));

        TableColumn<Account, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getType()));

        TableColumn<Account, String> customer = new TableColumn<>("Customer");
        customer.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getOwner().getName()));

        TableColumn<Account, String> phone = new TableColumn<>("Phone");
        phone.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getOwner().getPhone()));

        TableColumn<Account, String> balance = new TableColumn<>("Balance");
        balance.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%.2f", c.getValue().getBalance())));

        accountsTable.getColumns().setAll(java.util.List.of(number, type, customer, phone, balance));
        accountsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private ToolBar buildToolbar() {
        return new ToolBar(
                actionButton("Open Account", this::openAccount),
                actionButton("Deposit", this::deposit),
                actionButton("Withdraw", this::withdraw),
                actionButton("Transfer", this::transfer),
                actionButton("History", this::showHistory),
                actionButton("Apply Interest", this::applyInterest));
    }

    private Button actionButton(String label, BankAction action) {
        Button button = new Button(label);
        button.setOnAction(e -> {
            try {
                action.run();
            } catch (BankException ex) {
                showError(ex.getMessage());
            }
        });
        return button;
    }

    @FunctionalInterface
    private interface BankAction {
        void run() throws BankException;
    }

    // ---------- actions ----------

    private void openAccount() throws BankException {
        ComboBox<String> typeBox = new ComboBox<>(
                FXCollections.observableArrayList("SAVINGS", "CURRENT"));
        typeBox.getSelectionModel().selectFirst();
        TextField nameField = new TextField();
        TextField phoneField = new TextField();
        TextField balanceField = new TextField();

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new Insets(12));
        form.addRow(0, new Label("Account type:"), typeBox);
        form.addRow(1, new Label("Customer name:"), nameField);
        form.addRow(2, new Label("Phone:"), phoneField);
        form.addRow(3, new Label("Opening balance:"), balanceField);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Open Account");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        theme.apply(dialog.getDialogPane());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        Account account = bank.openAccount(
                typeBox.getValue(),
                nameField.getText().trim(),
                phoneField.getText().trim(),
                parseAmount(balanceField.getText()));
        refreshAccounts();
        info("Account created: " + account.getAccountNumber());
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
        TextInputDialog toDialog = new TextInputDialog();
        toDialog.setHeaderText("Transfer from " + from + " to account:");
        theme.apply(toDialog.getDialogPane());
        Optional<String> to = toDialog.showAndWait();
        if (to.isEmpty()) {
            return;
        }
        bank.transfer(from, to.get().trim(), promptAmount("Amount to transfer:"));
        refreshAccounts();
    }

    private void showHistory() throws BankException {
        String number = selectedAccountNumber();

        TableView<Transaction> table = new TableView<>(
                FXCollections.observableArrayList(bank.historyOf(number)));

        TableColumn<Transaction, String> time = new TableColumn<>("Date & Time");
        time.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getTimestamp().format(TIMESTAMP_FORMAT)));

        TableColumn<Transaction, Object> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getType()));

        TableColumn<Transaction, String> amount = new TableColumn<>("Amount");
        amount.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%.2f", c.getValue().getAmount())));

        TableColumn<Transaction, String> after = new TableColumn<>("Balance After");
        after.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%.2f", c.getValue().getBalanceAfter())));

        table.getColumns().setAll(java.util.List.of(time, type, amount, after));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("History of " + number);
        Scene historyScene = new Scene(new BorderPane(table), 580, 340);
        theme.apply(historyScene);
        dialog.setScene(historyScene);
        dialog.showAndWait();
    }

    private void applyInterest() throws BankException {
        double total = bank.applyInterestToSavings();
        refreshAccounts();
        info(String.format("Interest credited to all savings accounts: %.2f", total));
    }

    // ---------- helpers ----------

    private void refreshAccounts() {
        accountsTable.setItems(FXCollections.observableArrayList(bank.listAccounts()));
    }

    private String selectedAccountNumber() throws BankException {
        Account selected = accountsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            throw new BankException("Select an account in the table first.");
        }
        return selected.getAccountNumber();
    }

    private double promptAmount(String message) throws BankException {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText(message);
        theme.apply(dialog.getDialogPane());
        Optional<String> input = dialog.showAndWait();
        if (input.isEmpty()) {
            throw new BankException("Operation cancelled.");
        }
        return parseAmount(input.get());
    }

    private double parseAmount(String input) throws BankException {
        try {
            return Double.parseDouble(input.trim());
        } catch (NumberFormatException e) {
            throw new BankException("Not a valid number: " + input);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        theme.apply(alert.getDialogPane());
        alert.showAndWait();
    }

    private void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        theme.apply(alert.getDialogPane());
        alert.showAndWait();
    }
}
