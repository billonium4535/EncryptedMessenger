package com.example.encryptedmessenger;

import android.util.Base64;

import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;


/**
 * EncryptionHelper provides utility methods for:
 * <p>
 * 1. Deriving encryption keys for chat rooms.
 * <br>
 * 2. Encrypting messages with ChaCha20-Poly1305 AEAD.
 * <br>
 * 3. Decrypting messages previously encrypted.
 * </p>
 */
public final class EncryptionHelper {

    // Private constructor to prevent instantiation
    private EncryptionHelper() {}

    private static final SecureRandom RNG = new SecureRandom();

    /**
     * Generates a 16-byte salt from the room name.
     * <br>
     * Uses SHA-256 hash of the room and takes the first 16 bytes.
     *
     * @param room The chat room name.
     * @return 16-byte salt derived from room name.
     * @throws Exception If SHA-256 algorithm is unavailable.
     */
    private static byte[] salt16FromRoom(String room) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");

        // Hash room
        byte[] h = d.digest(room.getBytes(StandardCharsets.UTF_8));

        // Take first 16 bytes as salt
        return Arrays.copyOf(h, 16);
    }

    /**
     * Derives a 32-byte symmetric key for a room using script KDF.
     * <br>
     * Matches the Python implementation for compatibility.
     *
     * @param room The chat name.
     * @param passphrase User-provided passphrase for the room.
     * @return 32-byte derived key.
     * @throws Exception If key derivation fails.
     */
    public static byte[] deriveRoomKey(String room, String passphrase) throws Exception {
        // Generate salt from room name
        byte[] salt16 = salt16FromRoom(room);
        return SCrypt.generate(
                // Password bytes
                passphrase.getBytes(StandardCharsets.UTF_8),
                salt16,     // Salt
                16384,      // N parameter (CPU/memory cost)
                8,          // r parameter (block size)
                1,          // p parameter (parallelization)
                32          // Derived key length
        );
    }

    /**
     * Encrypts plaintext using ChaCha20-Poly1305 AEAD.
     * <br>
     * Returns Base64(NO_WRAP) encoding of nonce (12 bytes) || ciphertext+tag.
     *
     * @param key 32-byte encryption key.
     * @param plaintext The data to encrypt.
     * @param aad Additional authenticated data.
     * @return Base64-encoded ciphertext including nonce.
     * @throws Exception If encryption fails.
     */
    public static String encrypt(byte[] key, byte[] plaintext, byte[] aad) throws Exception {
        // 12-byte nonce for ChaCha20-Poly1305
        byte[] nonce = new byte[12];

        // Generate random nonce
        RNG.nextBytes(nonce);

        ChaCha20Poly1305 aead = new ChaCha20Poly1305();
        AEADParameters params = new AEADParameters(new KeyParameter(key), 128, nonce, aad);

        // Initialize AEAD for encryption
        aead.init(true, params);

        byte[] out = new byte[aead.getOutputSize(plaintext.length)];
        int off = aead.processBytes(plaintext, 0, plaintext.length, out, 0);
        int finalLen = off + aead.doFinal(out, off);

        // Combine nonce and ciphertext for transmission
        byte[] combined = new byte[12 + finalLen];
        System.arraycopy(nonce, 0, combined, 0, 12);
        System.arraycopy(out, 0, combined, 12, finalLen);

        // Return Base64-encoded string without line breaks
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    /**
     * Decrypts a Base64-encoded payload previously encrypted by this class.
     * <br>
     * Expects format: nonce (12 bytes) || ciphertext+tag.
     *
     * @param key 32-byte encryption key.
     * @param payloadB64 Base64(NO_WRAP) encoded encrypted data.
     * @param aad Additional authenticated data.
     * @return Decrypted plaintext bytes.
     * @throws Exception If decryption fails or ciphertext is invalid.
     */
    public static byte[] decrypt(byte[] key, String payloadB64, byte[] aad) throws Exception {
        // Decode Base64
        byte[] raw = Base64.decode(payloadB64, Base64.NO_WRAP);
        if (raw.length < 12 + 16) throw new IllegalArgumentException("ciphertext too short");

        // Split nonce and ciphertext
        byte[] nonce = new byte[12];
        byte[] ct    = new byte[raw.length - 12];
        System.arraycopy(raw, 0, nonce, 0, 12);
        System.arraycopy(raw, 12, ct, 0, ct.length);

        ChaCha20Poly1305 aead = new ChaCha20Poly1305();
        AEADParameters params = new AEADParameters(new KeyParameter(key), 128, nonce, aad);

        // Initialize AEAD for decryption
        aead.init(false, params);

        byte[] out = new byte[aead.getOutputSize(ct.length)];
        int off = aead.processBytes(ct, 0, ct.length, out, 0);
        int finalLen = off + aead.doFinal(out, off);

        // Return plaintext
        return Arrays.copyOf(out, finalLen);
    }
}
