package com.bank.model;

/**
 * Current (checking) account: no interest, but allows an overdraft.
 */
public class CurrentAccount extends Account {

    public static final double overdraftLimit = 10_000.0;

//    public CurrentAccount(String accountNumber, Customer owner, double openingBalance) {
//        super(accountNumber, owner, openingBalance);
//    }

    public CurrentAccount(String accountNumber, String accountHolderName, double openingBalance) {
        super(accountNumber, accountHolderName, openingBalance);
    }

    /** A current account may go negative down to the overdraft limit. */
    @Override
    public double withdrawableBalance() {
        return getBalance() + overdraftLimit;
    }

    @Override
    public String getType() {
        return "CURRENT";
    }

    @Override
    public String toString() {
        return String.format("%-10s | %-8s | %-28s | %12.2f | %-12.2f",
                getAccountNumber(), getType(), getAccountHolderName(), getBalance(), overdraftLimit);
    }
}
