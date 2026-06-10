package com.bank.model;

/**
 * Savings account: must keep a minimum balance, earns interest.
 */
public class SavingsAccount extends Account {

    public static final double MINIMUM_BALANCE = 500.0;
    public static final double INTEREST_RATE = 0.05; // 5% yearly

    public SavingsAccount(String accountNumber, Customer owner, double openingBalance) {
        super(accountNumber, owner, openingBalance);
    }

    /** A savings account may not drop below the minimum balance. */
    @Override
    public double withdrawableBalance() {
        return Math.max(0, getBalance() - MINIMUM_BALANCE);
    }

    @Override
    public String getType() {
        return "SAVINGS";
    }

    /** Applies one year's interest and returns the amount credited. */
    public double applyInterest() {
        double interest = getBalance() * INTEREST_RATE;
        credit(interest);
        return interest;
    }
}
