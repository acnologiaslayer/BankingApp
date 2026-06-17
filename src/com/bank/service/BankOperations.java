package com.bank.service;

import com.bank.exception.*;
import com.bank.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BankOperations {

    private final BankManagement bankManagement;
    private final Map<String, Account> accounts;
    private int nextAccountSequence;

    public BankOperations(BankManagement bankManagement) throws DataStoreException {
        this.bankManagement = bankManagement;
        this.accounts = new LinkedHashMap<>();
        for (Account account : bankManagement.loadAccounts()) {
            this.accounts.put(account.getAccountNumber(), account);
        }
        this.nextAccountSequence = highestExistingSequence() + 1;
    }

    /** Opens a new account and returns it. */
    public Account openAccount(String type, String name, double openingBalance)
            throws BankException {
        if (openingBalance < 0) {
            throw new InvalidAmountException("Opening balance cannot be negative.");
        }

        String number = nextAccountNumber();
        if (accounts.containsKey(number)) {
            throw new DuplicateAccountException(number);
        }


        Account account = switch (type) {
            case "SAVINGS" -> new SavingsAccount(number, name, openingBalance);
            case "CURRENT" -> new CurrentAccount(number, name, openingBalance);
            default -> throw new BankException("Unknown account type: " + type);
        };

        accounts.put(number, account);
        bankManagement.saveAccount(account);
        return account;
    }

    public void deposit(String accountNumber, double amount) throws BankException {
        Account account = requireAccount(accountNumber);
        account.deposit(amount);
        bankManagement.saveAccount(account);
    }

    public void withdraw(String accountNumber, double amount) throws BankException {
        Account account = requireAccount(accountNumber);
        account.withdraw(amount);
        bankManagement.saveAccount(account);
    }

    /** Moves money between two accounts atomically (in-memory). */
    public void transfer(String fromNumber, String toNumber, double amount)
            throws BankException {
        if (fromNumber.equals(toNumber)) {
            throw new BankException("Cannot transfer to the same account.");
        }
        Account from = requireAccount(fromNumber);
        Account to = requireAccount(toNumber);

        from.withdraw(amount); // validates amount and funds first
        to.deposit(amount);

        bankManagement.saveAccount(from);
        bankManagement.saveAccount(to);
    }

    public Account requireAccount(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    /** Returns every account, in creation order, for display purposes. */
    public List<Account> listAccounts() {
        return new ArrayList<>(accounts.values());
    }

    // ---------- helpers ----------

    private String nextAccountNumber() {
        return String.format("AC%05d", nextAccountSequence++);
    }

    private int highestExistingSequence() {
        int highest = 0;
        for (String number : accounts.keySet()) {
            try {
                highest = Math.max(highest, Integer.parseInt(number.substring(2)));
            } catch (NumberFormatException ignored) {
                // non-standard account number in the file; skip it
            }
        }
        return highest;
    }
}
