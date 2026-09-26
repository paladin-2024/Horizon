package com.horizon.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The transactional operations behind {@link RefreshTokenService}. */
@Component
class RefreshTokenStore {

    enum Status {
        ROTATED,
        NOT_FOUND,
        EXPIRED,
        REUSED
    }

    record Outcome(Status status, UUID userId, String token) {
    }

    private final RefreshTokenRepository repository;
    private final AuthProperties properties;

    RefreshTokenStore(RefreshTokenRepository repository, AuthProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    String startFamily(UUID userId) {
        Instant now = Instant.now();
        String token = Tokens.randomToken();
        repository.save(new RefreshToken(userId, UUID.randomUUID(), Tokens.sha256Hex(token),
                now.plus(properties.refreshTokenTtl()), now));
        return token;
    }

    @Transactional
    Outcome rotate(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            return new Outcome(Status.NOT_FOUND, null, null);
        }
        RefreshToken current = repository.findByTokenHash(Tokens.sha256Hex(presentedToken))
                .orElse(null);
        if (current == null) {
            return new Outcome(Status.NOT_FOUND, null, null);
        }
        Instant now = Instant.now();
        if (current.getRevokedAt() != null) {
            repository.revokeFamily(current.getFamilyId(), now);
            return new Outcome(Status.REUSED, current.getUserId(), null);
        }
        if (current.getExpiresAt().isBefore(now)) {
            return new Outcome(Status.EXPIRED, current.getUserId(), null);
        }
        current.revoke(now);
        String next = Tokens.randomToken();
        repository.save(new RefreshToken(current.getUserId(), current.getFamilyId(),
                Tokens.sha256Hex(next), now.plus(properties.refreshTokenTtl()), now));
        return new Outcome(Status.ROTATED, current.getUserId(), next);
    }

    @Transactional
    Optional<UUID> revokeFamilyOf(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            return Optional.empty();
        }
        return repository.findByTokenHash(Tokens.sha256Hex(presentedToken))
                .map(token -> {
                    repository.revokeFamily(token.getFamilyId(), Instant.now());
                    return token.getUserId();
                });
    }
}
