package service;

import dao.AccountDao;
import dao.TransactionDao;
import dao.UserDao;
import db.DataSourceManager;
import model.Account;
import model.BankTransaction;
import model.User;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class BankingService {

    private final AccountDao accountDao = new AccountDao();
    private final TransactionDao transactionDao = new TransactionDao();

    // Limites de sécurité
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("1000000.00");
    private static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");

    public List<Account> getAccountsForUser(int userId) throws SQLException {
        return accountDao.findByUserId(userId);
    }

    public Account createDefaultAccountForUser(int userId) throws SQLException {
        String accountNumber = "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return accountDao.createAccount(userId, accountNumber, BigDecimal.ZERO);
    }

    /**
     * Create a new account for an existing user by their auxiliary user_id.
     * This allows multiple accounts per user without creating duplicate user records.
     */
    public Account createAccountForUserId(String auxiliaryUserId) throws SQLException {
        UserDao userDao = new UserDao();
        User existingUser = userDao.findByUserId(auxiliaryUserId);
        if (existingUser == null) {
            throw new IllegalArgumentException("User ID not found: " + auxiliaryUserId);
        }
        String accountNumber = "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return accountDao.createAccount(existingUser.getId(), accountNumber, BigDecimal.ZERO);
    }

    /**
     * Create a new account for an existing user by their auxiliary user_id with password and PIN verification.
     * This ensures only authorized users can create additional accounts.
     */
    public Account createAccountForUserIdWithAuth(String auxiliaryUserId, String password, String pin) throws SQLException {
        UserDao userDao = new UserDao();
        User existingUser = userDao.findByUserId(auxiliaryUserId);
        if (existingUser == null) {
            throw new IllegalArgumentException("User ID not found: " + auxiliaryUserId);
        }

        // Verify password
        service.AuthService authService = new service.AuthService();
        boolean passwordValid = authService.verifyPassword(existingUser, password);
        if (!passwordValid) {
            throw new IllegalArgumentException("Invalid password");
        }

        // Verify PIN
        boolean pinValid = authService.verifyPin(existingUser, pin);
        if (!pinValid) {
            throw new IllegalArgumentException("Invalid PIN");
        }

        // If both verified, create the account
        String accountNumber = "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return accountDao.createAccount(existingUser.getId(), accountNumber, BigDecimal.ZERO);
    }

    /**
     * Transfer money between two accounts in a single DB transaction with row-level locking.
     */
    public void transfer(int fromAccountId, int toAccountId, BigDecimal amount, String description) throws SQLException {
        // Validate amount
        validateAmount(amount);

        // Sanitize and limit description
        description = sanitizeDescription(description);

        DataSource dataSource = DataSourceManager.getDataSource();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account from = accountDao.findByIdForUpdate(fromAccountId, conn);
                Account to = accountDao.findByIdForUpdate(toAccountId, conn);

                if (from == null || to == null) {
                    throw new IllegalArgumentException("Invalid account ID");
                }

                if (from.getBalance().compareTo(amount) < 0) {
                    throw new IllegalArgumentException("Insufficient funds");
                }

                BigDecimal newFromBalance = from.getBalance().subtract(amount);
                BigDecimal newToBalance = to.getBalance().add(amount);

                accountDao.updateBalance(from.getId(), newFromBalance, conn);
                accountDao.updateBalance(to.getId(), newToBalance, conn);

                transactionDao.createTransaction(from.getId(), to.getId(), amount, description, conn);

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }
                throw new SQLException("Transfer failed: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Deposit money into an account using JDBC transaction.
     */
    public void deposit(int accountId, BigDecimal amount, String description) throws SQLException {
        // Validate amount
        validateAmount(amount);

        // Sanitize and limit description
        description = sanitizeDescription(description);

        DataSource dataSource = DataSourceManager.getDataSource();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account account = accountDao.findByIdForUpdate(accountId, conn);

                if (account == null) {
                    throw new IllegalArgumentException("Account not found");
                }

                BigDecimal newBalance = account.getBalance().add(amount);
                accountDao.updateBalance(accountId, newBalance, conn);

                // Create transaction record (deposit: from_account_id is null)
                transactionDao.createTransaction(null, accountId, amount,
                        description != null ? description : "Deposit", conn);

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }
                throw new SQLException("Deposit failed: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Withdraw money from an account using JDBC transaction.
     */
    public void withdraw(int accountId, BigDecimal amount, String description) throws SQLException {
        // Validate amount
        validateAmount(amount);

        // Sanitize and limit description
        description = sanitizeDescription(description);

        DataSource dataSource = DataSourceManager.getDataSource();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account account = accountDao.findByIdForUpdate(accountId, conn);

                if (account == null) {
                    throw new IllegalArgumentException("Account not found");
                }

                if (account.getBalance().compareTo(amount) < 0) {
                    throw new IllegalArgumentException("Insufficient funds");
                }

                BigDecimal newBalance = account.getBalance().subtract(amount);
                accountDao.updateBalance(accountId, newBalance, conn);

                // Create transaction record (withdrawal: to_account_id is null)
                transactionDao.createTransaction(accountId, null, amount,
                        description != null ? description : "Withdrawal", conn);

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }
                throw new SQLException("Withdrawal failed: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Get all account numbers in the database.
     */
    public List<String> getAllAccountNumbers() throws SQLException {
        return accountDao.getAllAccountNumbers();
    }

    /**
     * Get transaction history for an account.
     */
    public List<BankTransaction> getTransactionHistory(int accountId) throws SQLException {
        return transactionDao.findByAccountId(accountId);
    }

    /**
     * Get account by account number.
     */
    public Account getAccountByNumber(String accountNumber) throws SQLException {
        return accountDao.findByAccountNumber(accountNumber);
    }

    /**
     * Get account by ID.
     */
    public Account getAccountById(int accountId) throws SQLException {
        return accountDao.findById(accountId);
    }

    /**
     * Validate transaction amount.
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(MIN_TRANSACTION_AMOUNT) < 0) {
            throw new IllegalArgumentException(
                    "Amount must be at least " + MIN_TRANSACTION_AMOUNT
            );
        }
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    "Amount cannot exceed " + MAX_TRANSACTION_AMOUNT
            );
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than 2 decimal places"
            );
        }
    }

    /**
     * Sanitize and limit description length for RSA encryption.
     * RSA-2048 can only encrypt ~245 bytes, so we limit descriptions.
     */
    private String sanitizeDescription(String description) {
        if (description == null || description.isEmpty()) {
            return null;
        }

        // Remove potentially dangerous characters
        description = description.replaceAll("[<>\"';\\\\]", "");
        description = description.trim();

        // Limit length for RSA encryption (important!)
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            description = description.substring(0, MAX_DESCRIPTION_LENGTH);
            System.out.println("⚠ Description truncated to " + MAX_DESCRIPTION_LENGTH + " characters");
        }

        return description;
    }
}