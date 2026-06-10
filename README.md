# BankingApp — Terminal Banking Application

A file-based banking system written in plain Java (no external libraries),
built as an IntelliJ IDEA project for MITM311 — Advanced OOP.

## How to run

**IntelliJ IDEA:** open the `BankingApp` folder as a project, then run
`com.bank.Main`.

**Command line:**

```sh
javac -d out $(find src -name "*.java")
java -cp out com.bank.Main
```

Data is stored in a `data/` directory (created automatically on first run).
Each file starts with a header line naming the columns:

- `accounts.txt` — `accountNumber;type;customerName;phone;balance`
- `transactions.txt` — append-only log: `accountNumber;type;amount;balanceAfter;timestamp`

## Features

1. Open account (Savings or Current)
2. Deposit / Withdraw / Transfer
3. Check balance and withdrawable amount
4. Per-account transaction history
5. List all accounts
6. Apply yearly interest to all savings accounts

Business rules: savings accounts must keep a 500.00 minimum balance and earn
5% yearly interest; current accounts allow a 10,000.00 overdraft.

## Design

```
com.bank
├── Main.java                  entry point — wires the layers together
├── ui/ConsoleUI.java          menu loop, input/output only
├── service/BankService.java   all business rules
├── repository/
│   ├── BankRepository.java    persistence interface (abstraction)
│   └── FileBankRepository.java java.nio.file.Files implementation
├── model/
│   ├── Account.java           abstract base class
│   ├── SavingsAccount.java    min-balance rule + interest
│   ├── CurrentAccount.java    overdraft rule
│   ├── Customer.java
│   ├── Transaction.java       immutable log record
│   └── TransactionType.java   enum
└── exception/
    ├── BankException.java     base checked exception
    ├── AccountNotFoundException.java
    ├── InsufficientFundsException.java
    ├── InvalidAmountException.java
    ├── DuplicateAccountException.java
    └── DataStoreException.java
```

### OOP principles demonstrated

- **Encapsulation** — `Account.balance` is private; it can only change via
  `deposit`/`withdraw`, which validate every operation.
- **Inheritance** — `SavingsAccount` and `CurrentAccount` extend the abstract
  `Account`; all custom exceptions extend `BankException`.
- **Polymorphism** — `withdrawableBalance()` is overridden per account type,
  so the same `withdraw` code enforces different rules; the service iterates
  `Account` references without caring about concrete types.
- **Abstraction** — `BankService` depends on the `BankRepository` interface,
  not on the file format, so storage could be swapped without touching
  business logic.

### Files & Collections

- `java.nio.file.Files` for creating, reading, writing, and appending data files.
- `LinkedHashMap<String, Account>` for O(1) account lookup in creation order,
  `ArrayList<Transaction>` for the history, `List.copyOf` /
  `Collections.unmodifiableList` for safe read-only views.
