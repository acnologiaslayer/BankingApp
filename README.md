# BankingApp — Terminal Banking Application

A file-based banking system written in plain Java (no external libraries),
built as an IntelliJ IDEA project for MITM311 — Advanced OOP.

## How to run

**IntelliJ IDEA:** open the `BankingApp` folder as a project, then run
`com.bank.Main`.

**Command line:**

```sh
# terminal UI (no dependencies)
javac -d out $(find src -name "*.java" -not -path "*/gui/*")
java -cp out com.bank.Main

# Qt GUI (this branch) — needs Qt 6.4 libraries installed
# (Ubuntu: sudo apt install qt6-base-dev)
mvn compile exec:java
```

The Qt GUI (`com.bank.gui.QtMain`) is built with
[Qt Jambi](https://github.com/OmixVisualization/qtjambi), the Java bindings
for Qt 6 — Maven pulls the bindings automatically, but the native Qt 6.4
libraries must be present on the system. It shows all accounts in a
QTableWidget and offers the same operations (open account, deposit,
withdraw, transfer, history, interest) through dialogs, reusing the exact
same `BankService` and `FileBankRepository` as the terminal UI.

## Usage guide (Qt GUI)

The main window shows every account in a QTableWidget with visible column
names (Account No, Type, Customer, Phone, Balance). Operations that act
on an existing account (Deposit, Withdraw, Transfer, History) need a row
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

The **Theme** menu (top-left) offers three looks, applied application-wide
(every window and dialog) through Qt stylesheets (QSS):

- **System** — your platform's native Qt style (default)
- **Dark** — dark palette
- **Ocean** — blue-tinted light palette

The stylesheets live in `QtTheme.java`; add your own theme by adding an
enum constant with a QSS string. The selected theme is saved to
`data/ui-settings.properties` and restored on the next start.

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
