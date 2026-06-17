package com.bank.service;

import com.bank.exception.DataStoreException;
import com.bank.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-store layer backed by an SQLite database (via JDBC).
 *
 * Replaces the previous CSV-file implementation. The public API
 * (loadAccounts / saveAccount) is unchanged so the service layer
 * above it does not need to know which storage technology is used.
 */
public class BankManagement {

    private static final String TABLE_NAME = "accounts";

    private final String url;

    public BankManagement(Path dataDirectory) throws DataStoreException {
        Path databaseFile = dataDirectory.resolve("bank.db");
        this.url = "jdbc:sqlite:" + databaseFile;
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new DataStoreException("Could not create data directory " + dataDirectory, e);
        }
        createTable();
        migrateLegacyCsv(dataDirectory.resolve("accounts.csv"));
    }

    // ---------- schema ----------

    private void createTable() throws DataStoreException {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
                + " account_number TEXT PRIMARY KEY,\n"
                + " type           TEXT NOT NULL,\n"
                + " customer_name  TEXT NOT NULL,\n"
                + " balance        REAL NOT NULL\n"
                + ");";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new DataStoreException("Could not initialise the database", e);
        }
    }

    // ---------- read ----------

    public ArrayList<Account> loadAccounts() throws DataStoreException {
        String sql = "SELECT account_number, type, customer_name, balance "
                + "FROM " + TABLE_NAME + " ORDER BY account_number";
        ArrayList<Account> accounts = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                accounts.add(toAccount(
                        rs.getString("account_number"),
                        rs.getString("type"),
                        rs.getString("customer_name"),
                        rs.getDouble("balance")));
            }
        } catch (SQLException e) {
            throw new DataStoreException("Could not load accounts from the database", e);
        }
        return accounts;
    }

    // ---------- write ----------

    /** Inserts a new account or updates it if the number already exists. */
    public void saveAccount(Account account) throws DataStoreException {
        String sql = "INSERT OR REPLACE INTO " + TABLE_NAME
                + " (account_number, type, customer_name, balance) VALUES (?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account.getAccountNumber());
            pstmt.setString(2, account.getType());
            pstmt.setString(3, account.getAccountHolderName());
            pstmt.setDouble(4, account.getBalance());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataStoreException("Could not save account " + account.getAccountNumber(), e);
        }
    }

    public void deleteAccount(String accountNumber) throws DataStoreException {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE account_number = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataStoreException("Could not delete account " + accountNumber, e);
        }
    }

    // ---------- helpers ----------

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private Account toAccount(String number, String type, String name, double balance)
            throws DataStoreException {
        return switch (type) {
            case "SAVINGS" -> new SavingsAccount(number, name, balance);
            case "CURRENT" -> new CurrentAccount(number, name, balance);
            default -> throw new DataStoreException("Unknown account type: " + type, null);
        };
    }

    /**
     * One-time import of the old semicolon-separated accounts.csv into SQLite,
     * so existing data is not lost when upgrading from the file-based version.
     * Runs only when the database is still empty and the legacy file exists.
     */
    private void migrateLegacyCsv(Path csvFile) throws DataStoreException {
        if (Files.notExists(csvFile) || !loadAccounts().isEmpty()) {
            return;
        }
        try {
            for (String line : Files.readAllLines(csvFile)) {
                if (line.isBlank() || line.startsWith("accountNumber")) {
                    continue;
                }
                String[] parts = line.split(";");
                if (parts.length < 4) {
                    continue;
                }
                saveAccount(toAccount(parts[0], parts[1], parts[2], Double.parseDouble(parts[3])));
            }
        } catch (IOException | NumberFormatException e) {
            throw new DataStoreException("Could not migrate legacy file " + csvFile, e);
        }
    }
}
