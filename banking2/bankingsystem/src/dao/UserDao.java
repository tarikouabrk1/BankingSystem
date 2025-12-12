package dao;

import db.DataSourceManager;
import model.User;

import javax.sql.DataSource;
import java.sql.*;

public class UserDao {

    private DataSource getDataSource() throws SQLException {
        return DataSourceManager.getDataSource();
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        DataSource dataSource = getDataSource();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    public User createUser(User user) throws SQLException {
        String sql = """
                INSERT INTO users (username, password_hash, password_salt, pin_hash, pin_salt, user_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        DataSource dataSource = getDataSource();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getPasswordSalt());
            ps.setString(4, user.getPinHash());
            ps.setString(5, user.getPinSalt());
            if (user.getUserId() == null || user.getUserId().isEmpty()) {
                ps.setNull(6, Types.VARCHAR);
            } else {
                ps.setString(6, user.getUserId());
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        }
        return user;
    }
    
    public User findByUserId(String userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ? LIMIT 1";
        DataSource dataSource = getDataSource();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setPasswordSalt(rs.getString("password_salt"));
        u.setPinHash(rs.getString("pin_hash"));
        u.setPinSalt(rs.getString("pin_salt"));
        String userId = rs.getString("user_id");
        if (rs.wasNull()) {
            userId = null;
        }
        u.setUserId(userId);
        return u;
    }
}


