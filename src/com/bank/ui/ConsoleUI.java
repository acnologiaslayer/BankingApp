package com.bank.ui;

import com.bank.exception.BankException;
import com.bank.model.Account;
import com.bank.service.BankOperations;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

/**
 * Terminal menu. Knows nothing about files or business rules —
 * it only reads input, calls BankService, and prints results.
 */
public class ConsoleUI {

    private final BankOperations bank;
    private final Scanner in;

    public ConsoleUI(BankOperations bank, Scanner in) {
        this.bank = bank;
        this.in = in;
    }

    /** Main loop: shows the menu until the user chooses to exit. */
    public void run() {
        System.out.println("===========================================");
        System.out.println("        AMAR BANK BANKING APPLICATION      ");
        System.out.println("===========================================");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ");
            try {
                switch (choice) {
                    case 1 -> openAccount();
                    case 2 -> deposit();
                    case 3 -> withdraw();
                    case 4 -> transfer();
                    case 5 -> checkBalance();
                    case 0 -> running = false;
                    default -> System.out.println("Unknown option, try again.");
                }
            } catch (BankException e) {
                // every domain error (insufficient funds, bad amount,
                // missing account, file problems...) lands here
                System.out.println("  [!] " + e.getMessage());
            }
            System.out.println();
        }
        System.out.println("Goodbye!");
    }

    private void printMenu() {
        System.out.println("-------------------------------------------");
        System.out.println(" 1. Open account");
        System.out.println(" 2. Deposit");
        System.out.println(" 3. Withdraw");
        System.out.println(" 4. Transfer");
        System.out.println(" 5. Check balance");
        System.out.println(" 0. Exit");
        System.out.println("-------------------------------------------");
    }

    // ---------- menu actions ----------

    private void openAccount() throws BankException {
        String type = readAccountType();
        String name = readLine("Customer name: ");
        double opening = readDouble("Opening balance: ");

        Account account = bank.openAccount(type, name, opening);
        System.out.println("  Account created: " + account);
    }

    private void deposit() throws BankException {
        String number = readLine("Account number: ");
        double amount = readDouble("Amount to deposit: ");
        bank.deposit(number, amount);
        System.out.printf("  Deposited %.2f. New balance: %.2f%n",
                amount, bank.requireAccount(number).getBalance());
    }

    private void withdraw() throws BankException {
        String number = readLine("Account number: ");
        double amount = readDouble("Amount to withdraw: ");
        bank.withdraw(number, amount);
        System.out.printf("  Withdrew %.2f. New balance: %.2f%n",
                amount, bank.requireAccount(number).getBalance());
    }

    private void transfer() throws BankException {
        String from = readLine("From account: ");
        String to = readLine("To account: ");
        double amount = readDouble("Amount to transfer: ");
        bank.transfer(from, to, amount);
        System.out.printf("  Transferred %.2f from %s to %s.%n", amount, from, to);
    }

    private void checkBalance() throws BankException {
        String number = readLine("Account number: ");
        Account account = bank.requireAccount(number);
        printAccountTable(List.of(account));
        System.out.printf("  Withdrawable right now: %.2f%n", account.withdrawableBalance());
    }

    /** Prints accounts as a table with a column-name header. */
    private void printAccountTable(List<Account> accounts) {
        String header = Account.tableHeader();
        System.out.println("  " + header);
        System.out.println("  " + "-".repeat(header.length()));
        for (Account account : accounts) {
            System.out.println("  " + account);
        }
    }

    // ---------- input helpers ----------

    private String readAccountType() {
        while (true) {
            String type = readLine("Account type (S = savings, C = current): ")
                    .trim().toUpperCase();
            if (type.startsWith("S")) return "SAVINGS";
            if (type.startsWith("C")) return "CURRENT";
            System.out.println("  Please enter S or C.");
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return in.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = in.nextInt();
                in.nextLine(); // consume the rest of the line
                return value;
            } catch (InputMismatchException e) {
                in.nextLine();
                System.out.println("  Please enter a whole number.");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = in.nextDouble();
                in.nextLine();
                return value;
            } catch (InputMismatchException e) {
                in.nextLine();
                System.out.println("  Please enter a number.");
            }
        }
    }
}
