package com.bank.repository;

import com.bank.exception.DataStoreException;
import com.bank.model.Account;
import com.bank.model.Transaction;

import java.util.List;
import java.util.Map;

/**
 * Persistence contract for the bank. The service layer depends on this
 * interface, not on a concrete storage mechanism (programming to an
 * abstraction), so the file store could be swapped for a database later.
 */
public interface BankRepository {

    /** Loads every account, keyed by account number. */
    Map<String, Account> loadAccounts() throws DataStoreException;

    /** Persists the full set of accounts. */
    void saveAccounts(Map<String, Account> accounts) throws DataStoreException;

    /** Loads the complete transaction log. */
    List<Transaction> loadTransactions() throws DataStoreException;

    /** Appends a single transaction to the log. */
    void appendTransaction(Transaction transaction) throws DataStoreException;
}
