package db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    /**
     * Ensure that the database schema and required tables exist.
     * This method can be safely called at application startup.
     */
    public static void initialize() throws SQLException {
        createDatabaseIfNotExists();
        createTablesIfNotExist();
        migrateSchema(); // Nouvelles migrations
    }

    private static void createDatabaseIfNotExists() throws SQLException {
        DataSource dataSource = DataSourceManager.getDataSourceWithoutDb();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            String sql = "CREATE DATABASE IF NOT EXISTS " + DatabaseConfig.DB_NAME;
            statement.executeUpdate(sql);
            System.out.println("✓ Database '" + DatabaseConfig.DB_NAME + "' verified/created successfully.");
        }
    }

    private static void createTablesIfNotExist() throws SQLException {
        DataSource dataSource = DataSourceManager.getDataSource();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Users table: stores login credentials and PIN hashes
            String createUsers = """
                    CREATE TABLE IF NOT EXISTS users (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(64) NOT NULL UNIQUE,
                        password_hash CHAR(64) NOT NULL,
                        password_salt CHAR(32) NOT NULL,
                        pin_hash CHAR(64) NOT NULL,
                        pin_salt CHAR(32) NOT NULL,
                        user_id VARCHAR(64),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_username (username),
                        INDEX idx_user_id (user_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;

            // Accounts table: bank accounts linked to a user
            String createAccounts = """
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        account_number VARCHAR(32) NOT NULL UNIQUE,
                        balance DECIMAL(15,2) NOT NULL DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        INDEX idx_account_number (account_number),
                        INDEX idx_user_id (user_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;

            // Transactions table: records transfers and operations (all sensitive data encrypted)
            String createTransactions = """
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        from_account_id_encrypted TEXT,
                        to_account_id_encrypted TEXT,
                        amount_encrypted TEXT NOT NULL,
                        description_encrypted TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_created_at (created_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;

            // RSA Keys table: stores RSA keys for encryption
            String createRSAKeys = """
                    CREATE TABLE IF NOT EXISTS rsa_keys (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        key_name VARCHAR(64) NOT NULL UNIQUE,
                        public_key_modulus TEXT NOT NULL,
                        public_key_exponent TEXT NOT NULL,
                        private_key_exponent TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_key_name (key_name)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;

            statement.executeUpdate(createUsers);
            System.out.println("✓ Table 'users' verified/created");

            statement.executeUpdate(createAccounts);
            System.out.println("✓ Table 'accounts' verified/created");

            statement.executeUpdate(createTransactions);
            System.out.println("✓ Table 'transactions' verified/created");

            statement.executeUpdate(createRSAKeys);
            System.out.println("✓ Table 'rsa_keys' verified/created");
        }
    }

    /**
     * Migrate existing schema to new version (add missing columns safely)
     */
    private static void migrateSchema() throws SQLException {
        DataSource dataSource = DataSourceManager.getDataSource();
        try (Connection connection = dataSource.getConnection()) {

            // Migration 1: Add user_id column to users table if missing
            if (!columnExists(connection, "users", "user_id")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE users ADD COLUMN user_id VARCHAR(64)");
                    statement.executeUpdate("CREATE INDEX idx_user_id ON users(user_id)");
                    System.out.println("✓ Migration: Added 'user_id' column to users table");
                }
            }

            // Migration 2: Add encrypted columns to transactions if missing
            if (!columnExists(connection, "transactions", "from_account_id_encrypted")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE transactions ADD COLUMN from_account_id_encrypted TEXT");
                    System.out.println("✓ Migration: Added 'from_account_id_encrypted' column");
                }
            }

            if (!columnExists(connection, "transactions", "to_account_id_encrypted")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE transactions ADD COLUMN to_account_id_encrypted TEXT");
                    System.out.println("✓ Migration: Added 'to_account_id_encrypted' column");
                }
            }

            if (!columnExists(connection, "transactions", "amount_encrypted")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE transactions ADD COLUMN amount_encrypted TEXT");
                    System.out.println("✓ Migration: Added 'amount_encrypted' column");
                }
            }

            if (!columnExists(connection, "transactions", "description_encrypted")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE transactions ADD COLUMN description_encrypted TEXT");
                    System.out.println("✓ Migration: Added 'description_encrypted' column");
                }
            }

            System.out.println("✓ Schema migrations completed");
        }
    }

    /**
     * Check if a column exists in a table (thread-safe)
     */
    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        String query = """
            SELECT COUNT(*) as count 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = ? 
            AND TABLE_NAME = ? 
            AND COLUMN_NAME = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, DatabaseConfig.DB_NAME);
            ps.setString(2, tableName);
            ps.setString(3, columnName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        }
        return false;
    }
}