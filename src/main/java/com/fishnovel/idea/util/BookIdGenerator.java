package com.fishnovel.idea.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class BookIdGenerator {
    private BookIdGenerator() {
    }

    public static String fromBytes(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}
