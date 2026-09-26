package com.horizon.user;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM for columns that must not be readable in a database dump.
 * The stored value is base64(iv || ciphertext || tag) with a fresh 12-byte IV per write.
 *
 * <p>JPA attribute converters are instantiated by Hibernate, not by Spring, so the single bean
 * publishes itself here and {@link NationalIdConverter} looks it up lazily on first conversion.
 */
@Component
public class FieldEncryptor {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static volatile FieldEncryptor instance;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    FieldEncryptor(@Value("${horizon.security.encryption-key}") String base64Key) {
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("horizon.security.encryption-key must be base64", e);
        }
        if (raw.length != 32) {
            throw new IllegalStateException(
                    "horizon.security.encryption-key must decode to 32 bytes, got " + raw.length);
        }
        this.key = new SecretKeySpec(raw, "AES");
        instance = this;
    }

    static FieldEncryptor instance() {
        FieldEncryptor current = instance;
        if (current == null) {
            throw new IllegalStateException("FieldEncryptor bean has not been created yet");
        }
        return current;
    }

    String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt the value", e);
        }
    }

    String decrypt(String stored) {
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            if (all.length <= IV_LENGTH) {
                throw new IllegalStateException("Stored ciphertext is too short");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not decrypt the value", e);
        }
    }
}
