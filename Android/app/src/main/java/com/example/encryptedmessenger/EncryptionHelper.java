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


public final class EncryptionHelper {

    private EncryptionHelper() {}

    private static final SecureRandom RNG = new SecureRandom();

    /** sha256(room) and take first 16 bytes for salt */
    private static byte[] salt16FromRoom(String room) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        byte[] h = d.digest(room.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(h, 16);
    }

    /** Derive 32-byte key with scrypt exactly like Python */
    public static byte[] deriveRoomKey(String room, String passphrase) throws Exception {
        byte[] salt16 = salt16FromRoom(room);
        return SCrypt.generate(
                passphrase.getBytes(StandardCharsets.UTF_8),
                salt16,
                16384, // N = 2^14
                8,     // r
                1,     // p
                32     // dkLen
        );
    }

    /**
     * Encrypt: returns Base64(NO_WRAP) of nonce(12) || ciphertext+tag
     */
    public static String encrypt(byte[] key, byte[] plaintext, byte[] aad) throws Exception {
        byte[] nonce = new byte[12];
        RNG.nextBytes(nonce);

        ChaCha20Poly1305 aead = new ChaCha20Poly1305();
        AEADParameters params = new AEADParameters(new KeyParameter(key), 128, nonce, aad);
        aead.init(true, params);

        byte[] out = new byte[aead.getOutputSize(plaintext.length)];
        int off = aead.processBytes(plaintext, 0, plaintext.length, out, 0);
        int finalLen = off + aead.doFinal(out, off);

        byte[] combined = new byte[12 + finalLen];
        System.arraycopy(nonce, 0, combined, 0, 12);
        System.arraycopy(out, 0, combined, 12, finalLen);

        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    /**
     * Decrypt: input is Base64(NO_WRAP) of nonce(12) || ciphertext+tag
     */
    public static byte[] decrypt(byte[] key, String payloadB64, byte[] aad) throws Exception {
        byte[] raw = Base64.decode(payloadB64, Base64.NO_WRAP);
        if (raw.length < 12 + 16) throw new IllegalArgumentException("ciphertext too short");

        byte[] nonce = new byte[12];
        byte[] ct    = new byte[raw.length - 12];
        System.arraycopy(raw, 0, nonce, 0, 12);
        System.arraycopy(raw, 12, ct, 0, ct.length);

        ChaCha20Poly1305 aead = new ChaCha20Poly1305();
        AEADParameters params = new AEADParameters(new KeyParameter(key), 128, nonce, aad);
        aead.init(false, params);

        byte[] out = new byte[aead.getOutputSize(ct.length)];
        int off = aead.processBytes(ct, 0, ct.length, out, 0);
        int finalLen = off + aead.doFinal(out, off);

        return Arrays.copyOf(out, finalLen);
    }
}
