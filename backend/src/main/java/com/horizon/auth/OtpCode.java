package com.horizon.auth;

import com.horizon.common.id.Uuid7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_codes", indexes = {
        @Index(name = "ix_otp_codes_phone_created_at", columnList = "phone, created_at"),
        @Index(name = "ix_otp_codes_expires_at", columnList = "expires_at")
})
class OtpCode {

    @Id
    private UUID id;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private OtpPurpose purpose;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OtpCode() {
    }

    OtpCode(String phone, String codeHash, OtpPurpose purpose, Instant expiresAt, Instant now) {
        this.id = Uuid7.next();
        this.phone = phone;
        this.codeHash = codeHash;
        this.purpose = purpose;
        this.attempts = 0;
        this.expiresAt = expiresAt;
        this.createdAt = now;
    }

    String getCodeHash() {
        return codeHash;
    }

    int getAttempts() {
        return attempts;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    void recordFailedAttempt() {
        this.attempts++;
    }

    void consume(Instant now) {
        this.consumedAt = now;
    }
}
