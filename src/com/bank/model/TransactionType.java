package com.bank.model;

/**
 * The kinds of operations recorded in the transaction log.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER_IN,
    TRANSFER_OUT,
    INTEREST,
    OPEN
}
