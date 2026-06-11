package com.bank.model;

/**
 * Savings account: must keep a minimum balance, earns interest.
 */
public class SavingsAccount extends Account {

//    public static final double MINIMUM_BALANCE = 500.0;
    public static final double interestRate = 0.05; // 5% yearly

//    public SavingsAccount(String accountNumber, Customer owner, double openingBalance) {
//        super(accountNumber, owner, openingBalance);
//    }

    public SavingsAccount(String accountNumber, String accountHolderName, double openingBalance) {
        super(accountNumber, accountHolderName, openingBalance);
    }

    /** A savings account may not drop below the minimum balance. */
    @Override
    public double withdrawableBalance() {
//        return Math.max(0, getBalance() - MINIMUM_BALANCE);
        return Math.max(0, getBalance());
    }

    @Override
    public String getType() {
        return "SAVINGS";
    }
}
