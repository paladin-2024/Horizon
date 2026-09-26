package com.horizon.user;

import com.horizon.common.id.Uuid7;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_phone", columnNames = "phone"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
class User {

    @Id
    private UUID id;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Convert(converter = NationalIdConverter.class)
    @Column(name = "national_id", length = 512)
    private String nationalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "country", nullable = false, length = 2)
    private Country country;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 2)
    private Language language;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    User(String phone, String passwordHash, String firstName, String lastName, String nationalId,
            Country country, Language language, String email, Instant now) {
        this.id = Uuid7.next();
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.nationalId = nationalId;
        this.country = country;
        this.language = language;
        this.email = email;
        this.createdAt = now;
        this.updatedAt = now;
    }

    UUID getId() {
        return id;
    }

    String getPhone() {
        return phone;
    }

    String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    String getFirstName() {
        return firstName;
    }

    String getLastName() {
        return lastName;
    }

    String getNationalId() {
        return nationalId;
    }

    Country getCountry() {
        return country;
    }

    Language getLanguage() {
        return language;
    }

    Instant getPhoneVerifiedAt() {
        return phoneVerifiedAt;
    }

    boolean isPhoneVerified() {
        return phoneVerifiedAt != null;
    }

    void markPhoneVerified(Instant now) {
        if (this.phoneVerifiedAt == null) {
            this.phoneVerifiedAt = now;
            this.updatedAt = now;
        }
    }
}
