package com.bank;

import com.bank.exception.DataStoreException;
import com.bank.service.BankManagement;
import com.bank.service.BankOperations;
import com.bank.ui.ConsoleUI;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Entry point: wires the layers together (repository -> service -> UI).
 */
public class Main {

    public static void main(String[] args) {
        try {
            BankManagement bankManagement = new BankManagement(Path.of("data"));
            BankOperations bank = new BankOperations(bankManagement);
            new ConsoleUI(bank, new Scanner(System.in)).run();
        } catch (DataStoreException e) {
            System.err.println("Could not start the bank: " + e.getMessage());
        }
    }
}
