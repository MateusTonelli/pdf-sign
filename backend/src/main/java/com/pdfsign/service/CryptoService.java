package com.pdfsign.service;

import com.pdfsign.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public CryptoService(AppProperties appProperties) {
        this.secretKey = deriveKey(appProperties.getCrypto().getSecret());
    }

    private static SecretKey deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive encryption key", e);
        }
    }

    public byte[] encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return combined;
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] encrypted) {
        try {
            if (encrypted.length < GCM_IV_LENGTH + 1) {
                throw new IllegalArgumentException("Invalid encrypted payload");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, GCM_IV_LENGTH);
            byte[] ciphertext = new byte[encrypted.length - GCM_IV_LENGTH];
            System.arraycopy(encrypted, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
