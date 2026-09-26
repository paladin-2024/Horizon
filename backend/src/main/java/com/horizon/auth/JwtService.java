package com.horizon.auth;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/** Issues and verifies the short-lived HS256 access token carried by the hz_access cookie. */
@Service
class JwtService {

    private static final String ISSUER_CLAIM = "iss";
    private static final String TOKEN_USE_CLAIM = "tok";
    private static final String ACCESS = "access";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final AuthProperties properties;

    JwtService(AuthProperties properties) {
        this.properties = properties;
        byte[] secret = Base64.getDecoder().decode(properties.jwtSecret());
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "horizon.auth.jwt-secret must decode to at least 32 bytes, got " + secret.length);
        }
        SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
        // The kid goes on the JWK, which is what puts it in the JWS header, so keys can rotate later.
        this.encoder = NimbusJwtEncoder.withSecretKey(key)
                .algorithm(MacAlgorithm.HS256)
                .jwkPostProcessor(builder -> builder.keyID(properties.jwtKid()))
                .build();
        NimbusJwtDecoder nimbusDecoder =
                NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        // "horizon" (our issuer value) is not a URL, but Spring's default claim-set converter tries
        // to parse "iss" as one and decode() throws IllegalArgumentException before any claim can be
        // read. The issuer is a plain identifier here, not a URI, so it is kept as a String instead.
        nimbusDecoder.setClaimSetConverter(
                MappedJwtClaimSetConverter.withDefaults(Map.of(ISSUER_CLAIM, value -> value)));
        this.decoder = nimbusDecoder;
    }

    String issueAccessToken(UUID userId) {
        return issueAccessTokenAt(userId, Instant.now());
    }

    String issueAccessTokenAt(UUID userId, Instant issuedAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim(TOKEN_USE_CLAIM, ACCESS)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** @return the user id when the token is a valid, unexpired access token issued by us. */
    Optional<UUID> verifyAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Jwt jwt = decoder.decode(token);
            if (!properties.issuer().equals(jwt.getClaimAsString(ISSUER_CLAIM))) {
                return Optional.empty();
            }
            if (!ACCESS.equals(jwt.getClaimAsString(TOKEN_USE_CLAIM))) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(jwt.getSubject()));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
