package dao;

import db.DataSourceManager;
import model.Account;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountDao {

    private DataSource getDataSource() throws SQLException {
        return DataSourceManager.getDataSource();
    }

    public List<Account> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE user_id = ?";
        List<Account> accounts = new ArrayList<>();
        DataSource dataSource = getDataSource();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapRow(rs));
                }
            }
        }
        return accounts;
    }

    public Account createAccount(int userId, String accountNumber, BigDecimal initialBalance) throws SQLException {
        String sql = """
                INSERT INTO accounts (user_id, account_number, balance)
                VALUES (?, ?, ?)
                """;
        Account account = new Account();
        account.setUserId(userId);
        account.setAccountNumber(accountNumber);
        account.setBalance(initialBalance);

        DataSource dataSource = getDataSource();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, accountNumber);
            ps.setBigDecimal(3, initialBalance);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    account.setId(keys.getInt(1));
                }
            }
        }
        return account;
    }

    public Account findByAccountNumber(String accountNumber) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        DataSource dataSource = getDataSource();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    public void updateBalance(int accountId, BigDecimal newBalance, Connection existingConn) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (PreparedStatement ps = existingConn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    public Account findByIdForUpdate(int accountId, Connection existingConn) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = existingConn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }
    
    public List<String> getAllAccountNumbers() throws SQLException {
        String sql = "SELECT account_number FROM accounts ORDER BY account_number";
        List<String> accountNumbers = new ArrayList<>();
        DataSource dataSource = getDataSource();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accountNumbers.add(rs.getString("account_number"));
                }
            }
        }
        return accountNumbers;
    }
    
    public Account findById(int accountId) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        DataSource dataSource = getDataSource();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getInt("id"));
        a.setUserId(rs.getInt("user_id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setBalance(rs.getBigDecimal("balance"));
        return a;
    }
}


