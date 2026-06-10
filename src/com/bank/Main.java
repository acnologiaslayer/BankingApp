package com.bank;

import com.bank.exception.DataStoreException;
import com.bank.repository.BankRepository;
import com.bank.repository.FileBankRepository;
import com.bank.service.BankService;
import com.bank.ui.ConsoleUI;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Entry point: wires the layers together (repository -> service -> UI).
 */
public class Main {

    public static void main(String[] args) {
        try {
            BankRepository repository = new FileBankRepository(Path.of("data"));
            BankService bank = new BankService(repository);
            new ConsoleUI(bank, new Scanner(System.in)).run();
        } catch (DataStoreException e) {
            System.err.println("Could not start the bank: " + e.getMessage());
        }
    }
}
