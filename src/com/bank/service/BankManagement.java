package com.bank.service;

import com.bank.exception.DataStoreException;
import com.bank.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BankManagement {

    private static final String SEPARATOR = ";";
    private static final String ACCOUNTS_HEADER =
            String.join(SEPARATOR, "accountNumber", "type", "customerName", "balance", "specialAttribute(Interest/Overdraft)");

    private final Path accountsFile;

    public BankManagement(Path dataDirectory) throws DataStoreException {
        this.accountsFile = dataDirectory.resolve("accounts.csv");
        try {
            Files.createDirectories(dataDirectory);
            if (Files.notExists(accountsFile)) {
                Files.write(accountsFile, List.of(ACCOUNTS_HEADER));
            }
        } catch (IOException e) {
            throw new DataStoreException("Could not initialise data files in " + dataDirectory, e);
        }
    }

    public ArrayList<Account> loadAccounts() throws DataStoreException {
        // LinkedHashMap keeps accounts in the order they were created.
        ArrayList<Account> accounts = new ArrayList<>();
        for (String line : readRecords(accountsFile)) {
            Account account = parseAccount(line);
            accounts.add(account);
        }
        return accounts;
    }

    public void saveAccounts(ArrayList<Account> accounts) throws DataStoreException {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(ACCOUNTS_HEADER);
        for (Account account : accounts) {
            lines.add(formatAccount(account));
        }
        try {
            Files.write(accountsFile, lines);
        } catch (IOException e) {
            throw new DataStoreException("Could not save accounts to " + accountsFile, e);
        }
    }

    // ---------- helpers ----------

    private List<String> readRecords(Path file) throws DataStoreException {
        try {
            List<String> lines = new ArrayList<>();
            for (String line : Files.readAllLines(file)) {
                if (!line.isBlank() && !line.equals(ACCOUNTS_HEADER)) {
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
                account.getAccountHolderName(),
                Double.toString(account.getBalance()),
                account.getType().equals("SAVINGS") ? ""+SavingsAccount.interestRate : ""+CurrentAccount.overdraftLimit);
    }

    private Account parseAccount(String line) throws DataStoreException {
        String[] parts = line.split(SEPARATOR);
        if (parts.length != 5) {
            throw new DataStoreException("Corrupt account record: " + line, null);
        }
        String number = parts[0];
        String type = parts[1];
        String accountHolderName = parts[2];
        double balance = Double.parseDouble(parts[3]);

        return switch (type) {
            case "SAVINGS" -> new SavingsAccount(number, accountHolderName, balance);
            case "CURRENT" -> new CurrentAccount(number, accountHolderName, balance);
            default -> throw new DataStoreException("Unknown account type: " + type, null);
        };
    }


}
