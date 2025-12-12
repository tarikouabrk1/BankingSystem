package service;

import dao.UserDao;
import model.User;
import security.auth.SecurityUtils;

import java.sql.SQLException;
import java.util.UUID;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public User register(String username, String password, String pin, String auxiliaryUserId) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (pin == null || pin.length() < 4) {
            throw new IllegalArgumentException("PIN must be at least 4 digits");
        }

        User user = new User();
        
        // If auxiliary user_id is provided, link to existing user
        if (auxiliaryUserId != null && !auxiliaryUserId.isBlank()) {
            User existingUser = userDao.findByUserId(auxiliaryUserId);
            if (existingUser == null) {
                throw new IllegalArgumentException("Auxiliary user ID not found. Please use a valid user_id or leave it empty to create a new user.");
            }
            user.setUserId(auxiliaryUserId);
            // Username must still be unique
            if (userDao.findByUsername(username) != null) {
                throw new IllegalArgumentException("Username already exists");
            }
        } else {
            // No auxiliary ID: username must be unique
            if (userDao.findByUsername(username) != null) {
                throw new IllegalArgumentException("Username already exists");
            }
            // Generate a random auxiliary ID for this user
            user.setUserId("UID-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        String passwordSalt = SecurityUtils.generateSalt(16);
        String passwordHash = SecurityUtils.hashPassword(password, passwordSalt);

        String pinSalt = SecurityUtils.generateSalt(16);
        String pinHash = SecurityUtils.hashPin(pin, pinSalt);

        user.setUsername(username);
        user.setPasswordSalt(passwordSalt);
        user.setPasswordHash(passwordHash);
        user.setPinSalt(pinSalt);
        user.setPinHash(pinHash);

        return userDao.createUser(user);
    }
    
    public User register(String username, String password, String pin) throws SQLException {
        return register(username, password, pin, null);
    }

    public User login(String username, String password) throws SQLException {
        User user = userDao.findByUsername(username);
        if (user == null) {
            return null;
        }

        boolean valid = SecurityUtils.verifyPassword(
                password,
                user.getPasswordSalt(),
                user.getPasswordHash()
        );
        return valid ? user : null;
    }

    public boolean verifyPin(User user, String pin) {
        return SecurityUtils.verifyPin(pin, user.getPinSalt(), user.getPinHash());
    }
    
    public boolean verifyPassword(User user, String password) {
        return SecurityUtils.verifyPassword(
                password,
                user.getPasswordSalt(),
                user.getPasswordHash()
        );
    }
}


