package com.bank.exception;

/**
 * Thrown when reading from or writing to the file-based
 * data store fails (wraps the underlying IOException).
 */
public class DataStoreException extends BankException {

    public DataStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
