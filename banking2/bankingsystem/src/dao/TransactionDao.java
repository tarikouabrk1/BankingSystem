package dao;

import db.DataSourceManager;
import model.BankTransaction;
import service.RSAKeyService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDao {

    private final RSAKeyService rsaKeyService = new RSAKeyService();

    public void createTransaction(Integer fromAccountId, Integer toAccountId, BigDecimal amount, String description, Connection existingConn) throws SQLException {
        String sql = """
                INSERT INTO transactions (from_account_id_encrypted, to_account_id_encrypted, amount_encrypted, description_encrypted)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = existingConn.prepareStatement(sql)) {
            // Encrypt account IDs
            String encryptedFromAccountId = null;
            if (fromAccountId != null) {
                encryptedFromAccountId = rsaKeyService.encrypt(String.valueOf(fromAccountId));
            }
            ps.setString(1, encryptedFromAccountId);
            
            String encryptedToAccountId = null;
            if (toAccountId != null) {
                encryptedToAccountId = rsaKeyService.encrypt(String.valueOf(toAccountId));
            }
            ps.setString(2, encryptedToAccountId);
            
            // Encrypt amount and description
            String encryptedAmount = rsaKeyService.encryptAmount(amount);
            String encryptedDescription = description != null ? rsaKeyService.encrypt(description) : null;
            
            ps.setString(3, encryptedAmount);
            ps.setString(4, encryptedDescription);
            
            ps.executeUpdate();
        }
    }
    
    public List<BankTransaction> findByAccountId(int accountId) throws SQLException {
        // Need to decrypt all transactions and filter by account ID
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        List<BankTransaction> transactions = new ArrayList<>();
        DataSource dataSource = DataSourceManager.getDataSource();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BankTransaction transaction = mapRow(rs);
                    // Filter by account ID after decryption
                    if ((transaction.getFromAccountId() != null && transaction.getFromAccountId().equals(accountId)) ||
                        (transaction.getToAccountId() != null && transaction.getToAccountId().equals(accountId))) {
                        transactions.add(transaction);
                    }
                }
            }
        }
        return transactions;
    }
    
    public List<BankTransaction> findAll() throws SQLException {
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        List<BankTransaction> transactions = new ArrayList<>();
        DataSource dataSource = DataSourceManager.getDataSource();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BankTransaction transaction = mapRow(rs);
                    transactions.add(transaction);
                }
            }
        }
        return transactions;
    }
    
    private BankTransaction mapRow(ResultSet rs) throws SQLException {
        BankTransaction t = new BankTransaction();
        t.setId(rs.getInt("id"));
        
        // Decrypt account IDs
        try {
            String encryptedFromId = rs.getString("from_account_id_encrypted");
            if (encryptedFromId != null && !encryptedFromId.isEmpty()) {
                String decryptedFromId = rsaKeyService.decrypt(encryptedFromId);
                t.setFromAccountId(Integer.parseInt(decryptedFromId));
            } else {
                // Try legacy unencrypted column if exists
                try {
                    Integer fromId = rs.getInt("from_account_id");
                    if (!rs.wasNull()) {
                        t.setFromAccountId(fromId);
                    }
                } catch (SQLException e) {
                    // Column doesn't exist, leave as null
                    t.setFromAccountId(null);
                }
            }
            
            String encryptedToId = rs.getString("to_account_id_encrypted");
            if (encryptedToId != null && !encryptedToId.isEmpty()) {
                String decryptedToId = rsaKeyService.decrypt(encryptedToId);
                t.setToAccountId(Integer.parseInt(decryptedToId));
            } else {
                // Try legacy unencrypted column if exists
                try {
                    Integer toId = rs.getInt("to_account_id");
                    if (!rs.wasNull()) {
                        t.setToAccountId(toId);
                    }
                } catch (SQLException e) {
                    // Column doesn't exist, leave as null
                    t.setToAccountId(null);
                }
            }
        } catch (Exception e) {
            // If decryption fails, try legacy columns
            try {
                Integer fromId = rs.getInt("from_account_id");
                if (!rs.wasNull()) {
                    t.setFromAccountId(fromId);
                }
                Integer toId = rs.getInt("to_account_id");
                if (!rs.wasNull()) {
                    t.setToAccountId(toId);
                }
            } catch (SQLException ex) {
                // Legacy columns don't exist, leave as null
            }
        }
        
        // Decrypt amount and description
        try {
            String encryptedAmount = rs.getString("amount_encrypted");
            if (encryptedAmount != null && !encryptedAmount.isEmpty()) {
                BigDecimal decryptedAmount = rsaKeyService.decryptAmount(encryptedAmount);
                t.setAmount(decryptedAmount);
            } else {
                // Try legacy unencrypted column if exists
                try {
                    t.setAmount(rs.getBigDecimal("amount"));
                } catch (SQLException e) {
                    throw new SQLException("No encrypted or unencrypted amount found");
                }
            }
            
            String encryptedDescription = rs.getString("description_encrypted");
            if (encryptedDescription != null && !encryptedDescription.isEmpty()) {
                String decryptedDescription = rsaKeyService.decrypt(encryptedDescription);
                t.setDescription(decryptedDescription);
            } else {
                // Try legacy unencrypted column if exists
                try {
                    t.setDescription(rs.getString("description"));
                } catch (SQLException e) {
                    // Description is optional
                    t.setDescription(null);
                }
            }
        } catch (Exception e) {
            throw new SQLException("Failed to decrypt transaction data: " + e.getMessage(), e);
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            t.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return t;
    }
}


