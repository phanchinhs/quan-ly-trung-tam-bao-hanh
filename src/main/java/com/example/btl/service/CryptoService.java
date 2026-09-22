package com.example.btl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class CryptoService {

    private static final Logger log = LoggerFactory.getLogger(CryptoService.class);
    public static final String PREFIX = "ENC:";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private static CryptoService instance;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public CryptoService(@Value("${app.crypto.secret:}") String secret) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalStateException(
                    "app.crypto.secret phải có ít nhất 16 ký tự (dùng để mã hóa dữ liệu nhạy cảm).");
        }
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Không thể khởi tạo khóa mã hóa", e);
        }
        CryptoService.instance = this;
    }

    public static CryptoService get() {
        return instance;
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(IV_LENGTH + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            log.error("Lỗi mã hóa dữ liệu", e);
            throw new IllegalStateException("Lỗi mã hóa dữ liệu", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        if (!stored.startsWith(PREFIX)) {
            return stored;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(all, 0, IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(all, IV_LENGTH, all.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Lỗi giải mã dữ liệu: {}", stored.substring(0, Math.min(20, stored.length())));
            return stored;
        }
    }

    public String sha256Hex(String value) {
        if (value == null) {
            return null;
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi băm dữ liệu", e);
        }
    }
}