package service;

import db.DataSourceManager;
import security.encryption.RSAEncryption;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RSAKeyService {

    private static final String SYSTEM_KEY_NAME = "SYSTEM_TRANSACTION_KEY";
    private static final int MAX_TEXT_BYTES = 200; // Limite de sécurité pour le texte

    // Cache pour éviter de recharger la clé à chaque fois
    private RSAEncryption.KeyPair cachedKeyPair = null;

    /**
     * Get or create the system RSA key pair for transaction encryption.
     */
    public RSAEncryption.KeyPair getOrCreateSystemKeyPair() throws SQLException {
        // Utiliser le cache si disponible
        if (cachedKeyPair != null) {
            return cachedKeyPair;
        }

        DataSource dataSource = DataSourceManager.getDataSource();
        try (Connection conn = dataSource.getConnection()) {
            // Try to retrieve existing key
            String selectSql = "SELECT public_key_modulus, public_key_exponent, private_key_exponent FROM rsa_keys WHERE key_name = ?";
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, SYSTEM_KEY_NAME);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigInteger modulus = new BigInteger(rs.getString("public_key_modulus"));
                        BigInteger publicExp = new BigInteger(rs.getString("public_key_exponent"));
                        BigInteger privateExp = new BigInteger(rs.getString("private_key_exponent"));
                        // CORRECTION: Utiliser le constructeur avec 3 paramètres seulement
                        cachedKeyPair = new RSAEncryption.KeyPair(modulus, publicExp, privateExp);
                        return cachedKeyPair;
                    }
                }
            }

            // Create new key pair if not found
            System.out.println("Generating RSA-2048 key pair for transaction encryption...");
            System.out.println("This may take a few seconds on first startup...");

            RSAEncryption.KeyPair keyPair = RSAEncryption.generateKeyPair(2048);

            String insertSql = "INSERT INTO rsa_keys (key_name, public_key_modulus, public_key_exponent, private_key_exponent) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, SYSTEM_KEY_NAME);
                ps.setString(2, keyPair.modulus.toString());
                ps.setString(3, keyPair.publicExponent.toString());
                ps.setString(4, keyPair.privateExponent.toString());
                ps.executeUpdate();
            }

            System.out.println("RSA key pair generated and stored successfully.");
            cachedKeyPair = keyPair;
            return keyPair;
        }
    }

    /**
     * Encrypt a string value using RSA.
     * IMPORTANT: Limited to MAX_TEXT_BYTES bytes to avoid exceeding RSA modulus size.
     */
    public String encrypt(String plaintext) throws SQLException {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }

        // Vérifier la taille AVANT de chiffrer
        byte[] plaintextBytes = plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        RSAEncryption.KeyPair keyPair = getOrCreateSystemKeyPair();

        // RSA-2048 peut chiffrer environ (2048/8) - 11 = 245 bytes max (avec PKCS#1 padding)
        // On limite à 200 bytes pour être sûr
        int maxBytes = (keyPair.modulus.bitLength() / 8) - 11;

        if (plaintextBytes.length > maxBytes) {
            throw new IllegalArgumentException(
                    String.format("Text too long for RSA encryption. Max %d bytes, got %d bytes. " +
                                    "Text: '%s...'",
                            maxBytes, plaintextBytes.length,
                            plaintext.substring(0, Math.min(50, plaintext.length())))
            );
        }

        BigInteger plaintextNumber = RSAEncryption.textToNumber(plaintext);

        // Vérifier que le nombre est inférieur au modulus
        if (plaintextNumber.compareTo(keyPair.modulus) >= 0) {
            throw new IllegalArgumentException(
                    String.format("Plaintext number too large. Number bits: %d, Modulus bits: %d",
                            plaintextNumber.bitLength(), keyPair.modulus.bitLength())
            );
        }

        BigInteger encrypted = RSAEncryption.encrypt(plaintextNumber, keyPair.publicExponent, keyPair.modulus);
        return encrypted.toString();
    }

    /**
     * Decrypt a string value using RSA.
     */
    public String decrypt(String ciphertext) throws SQLException {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return null;
        }

        try {
            RSAEncryption.KeyPair keyPair = getOrCreateSystemKeyPair();
            BigInteger ciphertextNumber = new BigInteger(ciphertext);

            // Vérifier que le ciphertext est dans la plage valide
            if (ciphertextNumber.compareTo(keyPair.modulus) >= 0) {
                throw new IllegalArgumentException("Ciphertext too large for modulus");
            }

            BigInteger decrypted = RSAEncryption.decrypt(ciphertextNumber, keyPair.privateExponent, keyPair.modulus);
            return RSAEncryption.numberToText(decrypted);
        } catch (NumberFormatException e) {
            throw new SQLException("Invalid ciphertext format: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypt a BigDecimal amount by converting to string first.
     * LIMIT: Amounts must be reasonable size (< 200 characters)
     */
    public String encryptAmount(BigDecimal amount) throws SQLException {
        if (amount == null) {
            return null;
        }

        String amountStr = amount.toPlainString();

        // Vérifier que le montant n'est pas trop long
        if (amountStr.length() > 50) { // Les montants bancaires dépassent rarement 50 caractères
            throw new IllegalArgumentException(
                    "Amount too large to encrypt: " + amountStr + " (" + amountStr.length() + " chars)"
            );
        }

        return encrypt(amountStr);
    }

    /**
     * Decrypt an amount string back to BigDecimal.
     */
    public BigDecimal decryptAmount(String encryptedAmount) throws SQLException {
        if (encryptedAmount == null || encryptedAmount.isEmpty()) {
            return null;
        }

        try {
            String decrypted = decrypt(encryptedAmount);
            return new BigDecimal(decrypted);
        } catch (NumberFormatException e) {
            throw new SQLException("Decrypted amount is not a valid number: " + e.getMessage(), e);
        }
    }
}