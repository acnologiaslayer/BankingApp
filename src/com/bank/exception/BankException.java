package com.bank.exception;

/**
 * Base class for all banking-related exceptions.
 * Allows callers to catch every domain error with a single catch block.
 */
public class BankException extends Exception {

    public BankException(String message) {
        super(message);
    }

    public BankException(String message, Throwable cause) {
        super(message, cause);
    }
}
