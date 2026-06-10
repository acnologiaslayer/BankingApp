package com.bank.model;

/**
 * Current (checking) account: no interest, but allows an overdraft.
 */
public class CurrentAccount extends Account {

    public static final double OVERDRAFT_LIMIT = 10_000.0;

    public CurrentAccount(String accountNumber, Customer owner, double openingBalance) {
        super(accountNumber, owner, openingBalance);
    }

    /** A current account may go negative down to the overdraft limit. */
    @Override
    public double withdrawableBalance() {
        return getBalance() + OVERDRAFT_LIMIT;
    }

    @Override
    public String getType() {
        return "CURRENT";
    }
}
