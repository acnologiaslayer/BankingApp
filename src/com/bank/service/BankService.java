package com.bank.service;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.BankException;
import com.bank.exception.DataStoreException;
import com.bank.exception.DuplicateAccountException;
import com.bank.exception.InvalidAmountException;
import com.bank.model.Account;
import com.bank.model.CurrentAccount;
import com.bank.model.Customer;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;
import com.bank.repository.BankRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * All business rules of the bank live here. The UI only talks to this
 * class, and this class only talks to the BankRepository abstraction.
 *
 * Collections used:
 *   Map<String, Account>  in-memory index of accounts by number
 *   List<Transaction>     full transaction history
 */
public class BankService {

    private final BankRepository repository;
    private final Map<String, Account> accounts;
    private final List<Transaction> transactions;
    private int nextAccountSequence;

    public BankService(BankRepository repository) throws DataStoreException {
        this.repository = repository;
        this.accounts = repository.loadAccounts();
        this.transactions = repository.loadTransactions();
        this.nextAccountSequence = highestExistingSequence() + 1;
    }

    /** Opens a new account and returns it. */
    public Account openAccount(String type, String name, String phone, double openingBalance)
            throws BankException {
        if (openingBalance < 0) {
            throw new InvalidAmountException("Opening balance cannot be negative.");
        }
        if ("SAVINGS".equals(type) && openingBalance < SavingsAccount.MINIMUM_BALANCE) {
            throw new InvalidAmountException(String.format(
                    "A savings account needs an opening balance of at least %.2f.",
                    SavingsAccount.MINIMUM_BALANCE));
        }

        String number = nextAccountNumber();
        if (accounts.containsKey(number)) {
            throw new DuplicateAccountException(number);
        }

        Customer owner = new Customer(name, phone);
        Account account = switch (type) {
            case "SAVINGS" -> new SavingsAccount(number, owner, openingBalance);
            case "CURRENT" -> new CurrentAccount(number, owner, openingBalance);
            default -> throw new BankException("Unknown account type: " + type);
        };

        accounts.put(number, account);
        record(account, TransactionType.OPEN, openingBalance);
        repository.saveAccounts(accounts);
        return account;
    }

    public void deposit(String accountNumber, double amount) throws BankException {
        Account account = requireAccount(accountNumber);
        account.deposit(amount);
        record(account, TransactionType.DEPOSIT, amount);
        repository.saveAccounts(accounts);
    }

    public void withdraw(String accountNumber, double amount) throws BankException {
        Account account = requireAccount(accountNumber);
        account.withdraw(amount);
        record(account, TransactionType.WITHDRAW, amount);
        repository.saveAccounts(accounts);
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

        record(from, TransactionType.TRANSFER_OUT, amount);
        record(to, TransactionType.TRANSFER_IN, amount);
        repository.saveAccounts(accounts);
    }

    /** Applies yearly interest to every savings account (polymorphism in action). */
    public double applyInterestToSavings() throws BankException {
        double total = 0;
        for (Account account : accounts.values()) {
            if (account instanceof SavingsAccount savings) {
                double interest = savings.applyInterest();
                record(savings, TransactionType.INTEREST, interest);
                total += interest;
            }
        }
        repository.saveAccounts(accounts);
        return total;
    }

    public Account requireAccount(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    /** Read-only view so callers cannot bypass the service. */
    public List<Account> listAccounts() {
        return List.copyOf(accounts.values());
    }

    /** History of one account, newest first. */
    public List<Transaction> historyOf(String accountNumber) throws AccountNotFoundException {
        requireAccount(accountNumber);
        List<Transaction> history = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t.getAccountNumber().equals(accountNumber)) {
                history.add(t);
            }
        }
        Collections.reverse(history);
        return Collections.unmodifiableList(history);
    }

    // ---------- helpers ----------

    private void record(Account account, TransactionType type, double amount)
            throws DataStoreException {
        Transaction transaction = new Transaction(
                account.getAccountNumber(), type, amount, account.getBalance());
        transactions.add(transaction);
        repository.appendTransaction(transaction);
    }

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
