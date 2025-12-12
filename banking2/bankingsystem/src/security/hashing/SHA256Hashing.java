package security.hashing;

/**
 * Pure-Java implementation of the SHA-256 hashing algorithm.
 * No use of java.security.MessageDigest so that the algorithm
 * itself is implemented from scratch.
 */
public final class SHA256Hashing {

    // Initial hash values (first 32 bits of the fractional parts of the square roots of the first 8 primes)
    private static final int[] H0 = {
            0x6a09e667,
            0xbb67ae85,
            0x3c6ef372,
            0xa54ff53a,
            0x510e527f,
            0x9b05688c,
            0x1f83d9ab,
            0x5be0cd19
    };

    // Round constants (first 32 bits of the fractional parts of the cube roots of the first 64 primes)
    private static final int[] K = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
            0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
            0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
            0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
            0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    private SHA256Hashing() {
    }

    public static String hash(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        byte[] bytes = input.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] digest = sha256(bytes);
        return toHex(digest);
    }

    private static byte[] sha256(byte[] message) {
        // 1. Pre-processing (padding)
        byte[] padded = padMessage(message);

        // 2. Initialize hash values
        int[] h = H0.clone();

        // 3. Process the message in successive 512-bit chunks
        int numBlocks = padded.length / 64;
        int[] w = new int[64];

        for (int i = 0; i < numBlocks; i++) {
            int offset = i * 64;

            // Prepare the message schedule w[0..63]
            for (int t = 0; t < 16; t++) {
                int index = offset + t * 4;
                w[t] = ((padded[index] & 0xff) << 24)
                        | ((padded[index + 1] & 0xff) << 16)
                        | ((padded[index + 2] & 0xff) << 8)
                        | (padded[index + 3] & 0xff);
            }
            for (int t = 16; t < 64; t++) {
                int s0 = smallSigma0(w[t - 15]);
                int s1 = smallSigma1(w[t - 2]);
                w[t] = w[t - 16] + s0 + w[t - 7] + s1;
            }

            // Initialize working variables with current hash value
            int a = h[0];
            int b = h[1];
            int c = h[2];
            int d = h[3];
            int e = h[4];
            int f = h[5];
            int g = h[6];
            int hh = h[7];

            // Main compression function
            for (int t = 0; t < 64; t++) {
                int S1 = bigSigma1(e);
                int ch = (e & f) ^ ((~e) & g);
                int temp1 = hh + S1 + ch + K[t] + w[t];
                int S0 = bigSigma0(a);
                int maj = (a & b) ^ (a & c) ^ (b & c);
                int temp2 = S0 + maj;

                hh = g;
                g = f;
                f = e;
                e = d + temp1;
                d = c;
                c = b;
                b = a;
                a = temp1 + temp2;
            }

            // Add the compressed chunk to the current hash value
            h[0] += a;
            h[1] += b;
            h[2] += c;
            h[3] += d;
            h[4] += e;
            h[5] += f;
            h[6] += g;
            h[7] += hh;
        }

        // Produce the final hash value (big-endian)
        byte[] output = new byte[32];
        for (int i = 0; i < 8; i++) {
            intToBytesBigEndian(h[i], output, i * 4);
        }
        return output;
    }

    private static byte[] padMessage(byte[] message) {
        int originalLength = message.length;
        long bitLength = (long) originalLength * 8L;

        // Append the bit '1' to the message, then k zero bits, then 64-bit length
        int numPaddingBytes = 64 - ((originalLength + 9) % 64);
        int totalLength = originalLength + 1 + numPaddingBytes + 8;

        byte[] padded = new byte[totalLength];
        System.arraycopy(message, 0, padded, 0, originalLength);

        // Append the '1' bit as 0x80
        padded[originalLength] = (byte) 0x80;

        // Last 8 bytes: original length in bits (big-endian)
        for (int i = 0; i < 8; i++) {
            padded[totalLength - 1 - i] = (byte) ((bitLength >>> (8 * i)) & 0xff);
        }

        return padded;
    }

    private static int rotateRight(int value, int bits) {
        return (value >>> bits) | (value << (32 - bits));
    }

    private static int bigSigma0(int x) {
        return rotateRight(x, 2) ^ rotateRight(x, 13) ^ rotateRight(x, 22);
    }

    private static int bigSigma1(int x) {
        return rotateRight(x, 6) ^ rotateRight(x, 11) ^ rotateRight(x, 25);
    }

    private static int smallSigma0(int x) {
        return rotateRight(x, 7) ^ rotateRight(x, 18) ^ (x >>> 3);
    }

    private static int smallSigma1(int x) {
        return rotateRight(x, 17) ^ rotateRight(x, 19) ^ (x >>> 10);
    }

    private static void intToBytesBigEndian(int value, byte[] dest, int offset) {
        dest[offset] = (byte) ((value >>> 24) & 0xff);
        dest[offset + 1] = (byte) ((value >>> 16) & 0xff);
        dest[offset + 2] = (byte) ((value >>> 8) & 0xff);
        dest[offset + 3] = (byte) (value & 0xff);
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}

