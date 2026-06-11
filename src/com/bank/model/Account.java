package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;

/**
 * Abstract base of every account type.
 * Demonstrates:
 *  - Abstraction:   withdrawal rules are deferred to subclasses.
 *  - Encapsulation: balance can only change through deposit/withdraw.
 *  - Polymorphism:  the service layer works with Account references
 *                   without knowing the concrete type.
 */
public abstract class Account {

    private final String accountNumber;
//    private final Customer accountHolderName;
    private final String accountHolderName;
    private double balance;

//    protected Account(String accountNumber, Customer owner, double openingBalance) {
    protected Account(String accountNumber, String owner, double openingBalance) {
        this.accountNumber = accountNumber;
        this.accountHolderName = owner;
        this.balance = openingBalance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

//    public Customer getOwner() {
//        return accountHolderName;
//    }

//    public String getOwner() {
//        return accountHolderName;
//    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public double getBalance() {
        return balance;
    }

    /** Adds money to the account after validating the amount. */
    public void deposit(double amount) throws InvalidAmountException {
        validateAmount(amount);
        balance += amount;
    }

    /** Removes money, enforcing the subclass-specific withdrawal rule. */
    public void withdraw(double amount)
            throws InvalidAmountException, InsufficientFundsException {
        validateAmount(amount);
        if (amount > withdrawableBalance()) {
            throw new InsufficientFundsException(accountNumber, amount, withdrawableBalance());
        }
        balance -= amount;
    }

    private void validateAmount(double amount) throws InvalidAmountException {
        if (Double.isNaN(amount) || amount <= 0) {
            throw new InvalidAmountException(amount);
        }
    }

    /** How much money may actually be withdrawn right now. */
    public abstract double withdrawableBalance();

    /** Short code used in menus and in the data file ("SAVINGS"/"CURRENT"). */
    public abstract String getType();

    /** Column names aligned with toString(), for table displays. */
    public static String tableHeader() {
        return String.format("%-10s | %-8s | %-28s | %12s",
                "ACCOUNT NO", "TYPE", "CUSTOMER", "BALANCE");
    }

    @Override
    public String toString() {
        return String.format("%-10s | %-8s | %-28s | %12.2f",
                accountNumber, getType(), accountHolderName, balance);
    }
}
