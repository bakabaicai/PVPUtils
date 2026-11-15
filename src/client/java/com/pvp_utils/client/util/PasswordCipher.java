package com.pvp_utils.client.util;

import java.io.IOException;

public final class PasswordCipher {
    private PasswordCipher() {
    }

    public static String encrypt(String plainText) {
        try {
            return CredentialProtection.protect(plainText);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot encrypt the login password", e);
        }
    }

    public static String decrypt(String stored) {
        try {
            return CredentialProtection.unprotect(stored);
        } catch (IOException e) {
            return "";
        }
    }

    public static boolean isEncrypted(String stored) {
        return CredentialProtection.isProtected(stored);
    }

}
