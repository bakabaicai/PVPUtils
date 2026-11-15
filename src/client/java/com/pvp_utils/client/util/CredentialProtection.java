package com.pvp_utils.client.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class CredentialProtection {
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CredentialProtection() {
    }

    public static boolean isProtected(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public static String protect(String value) throws IOException {
        if (value == null || value.isEmpty()) return "";
        byte[] plain = value.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain);
            byte[] combined = Arrays.copyOf(iv, iv.length + encrypted.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException | RuntimeException error) {
            throw new IOException("Credential encryption failed", error);
        } finally {
            Arrays.fill(plain, (byte) 0);
        }
    }

    public static String unprotect(String value) throws IOException {
        if (value == null || value.isEmpty()) return "";
        if (!isProtected(value)) throw new IOException("Unprotected credential rejected");
        byte[] plain = null;
        try {
            byte[] combined = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            if (combined.length < IV_LENGTH + 16) throw new IOException("Invalid encrypted credential");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, combined, 0, IV_LENGTH));
            plain = cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | RuntimeException error) {
            throw new IOException("Credential decryption failed", error);
        } finally {
            if (plain != null) Arrays.fill(plain, (byte) 0);
        }
    }

    public static void writeAtomically(Path path, String contents) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), ".pvputils-credentials-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer bytes = StandardCharsets.UTF_8.encode(contents);
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temporary, path.toAbsolutePath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static SecretKeySpec key() throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update("PVPUtils-ServerAutoLogin-Salt".getBytes(StandardCharsets.UTF_8));
        String fingerprint = System.getProperty("user.name", "") + "|" + System.getProperty("user.home", "");
        digest.update(fingerprint.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(digest.digest(), 16), "AES");
    }

}
