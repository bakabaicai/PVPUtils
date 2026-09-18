package com.pvp_utils.client.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class PasswordCipher {
    private static final String PREFIX = "enc:v1:";
    private static final byte[] SALT = "PVPUtils-ServerAutoLogin-Salt".getBytes(StandardCharsets.UTF_8);
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordCipher() {
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            return "";
        }
    }

    public static String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return "";
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (combined.length <= IV_LENGTH) {
                return "";
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, combined, 0, IV_LENGTH));
            byte[] decrypted = cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isEncrypted(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    private static SecretKeySpec key() {
        try {
            String fingerprint = System.getProperty("user.name", "") + "|" + System.getProperty("user.home", "");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(SALT);
            digest.update(fingerprint.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(Arrays.copyOf(digest.digest(), 16), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Password cipher key unavailable", e);
        }
    }
}
