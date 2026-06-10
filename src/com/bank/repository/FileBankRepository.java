package com.bank.repository;

import com.bank.exception.DataStoreException;
import com.bank.model.Account;
import com.bank.model.CurrentAccount;
import com.bank.model.Customer;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores bank data in two plain-text files under ./data.
 * Each file starts with a header line naming the columns, followed by
 * one record per line (semicolon-separated):
 *
 *   accounts.txt      accountNumber;type;customerName;phone;balance
 *   transactions.txt  accountNumber;type;amount;balanceAfter;timestamp
 *
 * Uses java.nio.file.Files for all reading and writing.
 */
public class FileBankRepository implements BankRepository {

    private static final String SEPARATOR = ";";
    private static final String ACCOUNTS_HEADER =
            String.join(SEPARATOR, "accountNumber", "type", "customerName", "phone", "balance");
    private static final String TRANSACTIONS_HEADER =
            String.join(SEPARATOR, "accountNumber", "type", "amount", "balanceAfter", "timestamp");
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Path accountsFile;
    private final Path transactionsFile;

    public FileBankRepository(Path dataDirectory) throws DataStoreException {
        this.accountsFile = dataDirectory.resolve("accounts.txt");
        this.transactionsFile = dataDirectory.resolve("transactions.txt");
        try {
            Files.createDirectories(dataDirectory);
            if (Files.notExists(accountsFile)) {
                Files.write(accountsFile, List.of(ACCOUNTS_HEADER));
            }
            if (Files.notExists(transactionsFile)) {
                Files.write(transactionsFile, List.of(TRANSACTIONS_HEADER));
            }
        } catch (IOException e) {
            throw new DataStoreException("Could not initialise data files in " + dataDirectory, e);
        }
    }

    @Override
    public Map<String, Account> loadAccounts() throws DataStoreException {
        // LinkedHashMap keeps accounts in the order they were created.
        Map<String, Account> accounts = new LinkedHashMap<>();
        for (String line : readRecords(accountsFile, ACCOUNTS_HEADER)) {
            Account account = parseAccount(line);
            accounts.put(account.getAccountNumber(), account);
        }
        return accounts;
    }

    @Override
    public void saveAccounts(Map<String, Account> accounts) throws DataStoreException {
        List<String> lines = new ArrayList<>();
        lines.add(ACCOUNTS_HEADER);
        for (Account account : accounts.values()) {
            lines.add(formatAccount(account));
        }
        try {
            Files.write(accountsFile, lines);
        } catch (IOException e) {
            throw new DataStoreException("Could not save accounts to " + accountsFile, e);
        }
    }

    @Override
    public List<Transaction> loadTransactions() throws DataStoreException {
        List<Transaction> transactions = new ArrayList<>();
        for (String line : readRecords(transactionsFile, TRANSACTIONS_HEADER)) {
            transactions.add(parseTransaction(line));
        }
        return transactions;
    }

    @Override
    public void appendTransaction(Transaction transaction) throws DataStoreException {
        String line = formatTransaction(transaction) + System.lineSeparator();
        try {
            Files.writeString(transactionsFile, line, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new DataStoreException("Could not append transaction to " + transactionsFile, e);
        }
    }

    // ---------- helpers ----------

    /** Reads all data lines from a file, skipping blanks and the header row. */
    private List<String> readRecords(Path file, String header) throws DataStoreException {
        try {
            List<String> lines = new ArrayList<>();
            for (String line : Files.readAllLines(file)) {
                if (!line.isBlank() && !line.equals(header)) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (IOException e) {
            throw new DataStoreException("Could not read " + file, e);
        }
    }

    private String formatAccount(Account account) {
        return String.join(SEPARATOR,
                account.getAccountNumber(),
                account.getType(),
                account.getOwner().getName(),
                account.getOwner().getPhone(),
                Double.toString(account.getBalance()));
    }

    private Account parseAccount(String line) throws DataStoreException {
        String[] parts = line.split(SEPARATOR);
        if (parts.length != 5) {
            throw new DataStoreException("Corrupt account record: " + line, null);
        }
        String number = parts[0];
        String type = parts[1];
        Customer owner = new Customer(parts[2], parts[3]);
        double balance = Double.parseDouble(parts[4]);

        return switch (type) {
            case "SAVINGS" -> new SavingsAccount(number, owner, balance);
            case "CURRENT" -> new CurrentAccount(number, owner, balance);
            default -> throw new DataStoreException("Unknown account type: " + type, null);
        };
    }

    private String formatTransaction(Transaction t) {
        return String.join(SEPARATOR,
                t.getAccountNumber(),
                t.getType().name(),
                Double.toString(t.getAmount()),
                Double.toString(t.getBalanceAfter()),
                t.getTimestamp().format(TIMESTAMP_FORMAT));
    }

    private Transaction parseTransaction(String line) throws DataStoreException {
        String[] parts = line.split(SEPARATOR);
        if (parts.length != 5) {
            throw new DataStoreException("Corrupt transaction record: " + line, null);
        }
        return new Transaction(
                parts[0],
                TransactionType.valueOf(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                LocalDateTime.parse(parts[4], TIMESTAMP_FORMAT));
    }
}
