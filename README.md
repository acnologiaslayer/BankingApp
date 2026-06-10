# BankingApp — Terminal Banking Application

A file-based banking system written in plain Java (no external libraries),
built as an IntelliJ IDEA project for MITM311 — Advanced OOP.

## How to run

**IntelliJ IDEA:** open the `BankingApp` folder as a project (reload the
Maven project if prompted), then run `com.bank.gui.SwingMain` for the GUI
or `com.bank.Main` for the terminal UI. No dependencies needed — Swing
ships with the JDK.

**After switching branches** in a running IDE, do **Maven → Reload
Project** and **Build → Rebuild Project** once, so the build output
matches the branch you are on.

**Command line:**

```sh
javac -d out $(find src -name "*.java")
java -cp out com.bank.Main            # terminal UI
java -cp out com.bank.gui.SwingMain   # Swing GUI (this branch)
```

The Swing GUI (`com.bank.gui`) shows all accounts in a table and offers
the same operations (open account, deposit, withdraw, transfer, history,
interest) through dialogs. It reuses the exact same `BankService` and
`FileBankRepository` as the terminal UI — only the presentation differs.

## Usage guide (Swing GUI)

The main window shows every account in a table with visible column names
(Account No, Type, Customer, Phone, Balance). Operations that act on an
existing account (Deposit, Withdraw, Transfer, History) need a row
**selected in the table first** — click the row, then the button.

| Button | What it does |
|---|---|
| **Open Account** | Form dialog: choose SAVINGS or CURRENT, enter name, phone, opening balance. Savings needs at least 500.00. |
| **Deposit** | Asks for an amount and credits the selected account. |
| **Withdraw** | Asks for an amount. Savings can't go below 500.00; current can overdraw to −10,000.00. |
| **Transfer** | Asks for the target account number, then the amount. |
| **History** | Opens a dialog listing the selected account's transactions, newest first. |
| **Apply Interest** | Credits 5% yearly interest to every savings account. |

Invalid input (bad amounts, insufficient funds, unknown accounts) shows an
error dialog — the message comes from the same custom exceptions the
terminal UI uses.

### Theming

The **Theme** menu (top-left) offers four looks:

- **System** — your OS native Look&Feel (default)
- **Metal** — classic cross-platform Swing
- **Nimbus** — modern light theme
- **Nimbus Dark** — Nimbus with a dark colour palette

The theme switches live and your choice is saved to
`data/ui-settings.properties`, so it is restored on the next start.

## Data files

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
