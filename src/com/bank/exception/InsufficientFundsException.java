package com.bank.exception;

/**
 * Thrown when a withdrawal or transfer would push an account
 * below its allowed minimum (or overdraft) limit.
 */
public class InsufficientFundsException extends BankException {

    public InsufficientFundsException(String accountNumber, double requested, double available) {
        super(String.format(
                "Insufficient funds in account %s: requested %.2f but only %.2f is available.",
                accountNumber, requested, available));
    }
}
