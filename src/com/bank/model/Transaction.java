package com.bank.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable record of a single account operation.
 */
public class Transaction {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String accountNumber;
    private final TransactionType type;
    private final double amount;
    private final double balanceAfter;
    private final LocalDateTime timestamp;

    public Transaction(String accountNumber, TransactionType type,
                       double amount, double balanceAfter, LocalDateTime timestamp) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
    }

    public Transaction(String accountNumber, TransactionType type,
                       double amount, double balanceAfter) {
        this(accountNumber, type, amount, balanceAfter, LocalDateTime.now());
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /** Column names aligned with toString(), for table displays. */
    public static String tableHeader() {
        return String.format("%-19s | %-12s | %10s | %13s",
                "DATE & TIME", "TYPE", "AMOUNT", "BALANCE AFTER");
    }

    @Override
    public String toString() {
        return String.format("%-19s | %-12s | %10.2f | %13.2f",
                timestamp.format(FORMAT), type, amount, balanceAfter);
    }
}
