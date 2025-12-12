package security.encryption;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Minimal RSA implementation for educational purposes.
 * This class does NOT use Java's built-in crypto APIs
 * (like javax.crypto) and instead performs the core math
 * using BigInteger.
 */
public final class RSAEncryption {

    public static final class KeyPair {
        public final BigInteger modulus;  // n
        public final BigInteger publicExponent;  // e
        public final BigInteger privateExponent; // d

        public KeyPair(BigInteger modulus, BigInteger publicExponent, BigInteger privateExponent) {
            this.modulus = modulus;
            this.publicExponent = publicExponent;
            this.privateExponent = privateExponent;
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private RSAEncryption() {
    }

    /**
     * Generate an RSA key pair with the given key size.
     * Common sizes: 1024, 2048.
     */
    public static KeyPair generateKeyPair(int keySize) {
        if (keySize < 512) {
            throw new IllegalArgumentException("Key size too small; use at least 512 bits for demo, 2048 bits for real security.");
        }

        int primeSize = keySize / 2;
        BigInteger p = BigInteger.probablePrime(primeSize, RANDOM);
        BigInteger q = BigInteger.probablePrime(primeSize, RANDOM);

        BigInteger n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));

        BigInteger e = BigInteger.valueOf(65537); // standard public exponent
        if (!phi.gcd(e).equals(BigInteger.ONE)) {
            // regenerate if e is not coprime with phi
            return generateKeyPair(keySize);
        }

        BigInteger d = e.modInverse(phi);

        return new KeyPair(n, e, d);
    }

    public static BigInteger encrypt(BigInteger plaintext, BigInteger publicExponent, BigInteger modulus) {
        if (plaintext.compareTo(BigInteger.ZERO) < 0 || plaintext.compareTo(modulus) >= 0) {
            throw new IllegalArgumentException("Plaintext out of range");
        }
        return plaintext.modPow(publicExponent, modulus);
    }

    public static BigInteger decrypt(BigInteger ciphertext, BigInteger privateExponent, BigInteger modulus) {
        if (ciphertext.compareTo(BigInteger.ZERO) < 0 || ciphertext.compareTo(modulus) >= 0) {
            throw new IllegalArgumentException("Ciphertext out of range");
        }
        return ciphertext.modPow(privateExponent, modulus);
    }

    /**
     * Convenience helpers for short text messages.
     * These are NOT padded and only for demonstration.
     */
    public static BigInteger textToNumber(String text) {
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new BigInteger(1, bytes);
    }

    public static String numberToText(BigInteger number) {
        byte[] bytes = number.toByteArray();
        // BigInteger may add a leading zero byte for sign; strip it
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            bytes = tmp;
        }
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}

