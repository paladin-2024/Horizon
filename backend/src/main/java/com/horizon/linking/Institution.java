package com.horizon.linking;

import com.horizon.common.id.Uuid7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * A bank or mobile money wallet a user can link an account at. Seeded on startup; never created by users.
 *
 * <p>Implements {@link Persistable} because the id is assigned in the constructor: without it Spring Data
 * would treat every new instance as detached and issue a SELECT-then-INSERT through {@code merge}.
 */
@Entity
@Table(name = "institutions",
        uniqueConstraints = @UniqueConstraint(name = "uk_institutions_country_code",
                columnNames = {"country", "code"}))
class Institution implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private InstitutionType type;

    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Transient
    private boolean isNew = true;

    protected Institution() {
    }

    Institution(String country, String code, String name, InstitutionType type) {
        this.id = Uuid7.next();
        this.country = country;
        this.code = code;
        this.name = name;
        this.type = type;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    String getName() {
        return name;
    }

    InstitutionType getType() {
        return type;
    }

    String getCountry() {
        return country;
    }

    String getCode() {
        return code;
    }

    void rename(String name) {
        this.name = name;
    }

    void retype(InstitutionType type) {
        this.type = type;
    }
}
