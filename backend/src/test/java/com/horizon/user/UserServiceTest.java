package com.horizon.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static String uniquePhone() {
        return "+25677" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private NewUser sample(String phone, String email, String nationalId) {
        return new NewUser(phone, "$argon2id$fake", "Ada", "Lovelace", "UG", "en", email, nationalId);
    }

    @Test
    void createsAndReadsBackAUser() {
        String phone = uniquePhone();
        String email = uniqueEmail();

        UserView created = userService.create(sample(phone, email, "CM123456"));

        assertThat(created.id()).isNotNull();
        assertThat(created.phone()).isEqualTo(phone);
        assertThat(created.email()).isEqualTo(email);
        assertThat(created.country()).isEqualTo("UG");
        assertThat(created.language()).isEqualTo("en");
        assertThat(created.phoneVerified()).isFalse();

        UserView loaded = userService.findById(created.id()).orElseThrow();
        assertThat(loaded).isEqualTo(created);
        assertThat(userService.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void storesEmailLowercasedAndFindsCredentialsByPhoneOrEmail() {
        String phone = uniquePhone();
        String email = uniqueEmail();

        UserView created = userService.create(sample(phone, email.toUpperCase(java.util.Locale.ROOT), null));

        assertThat(created.email()).isEqualTo(email.toLowerCase(java.util.Locale.ROOT));
        UserCredentials byPhone = userService.findCredentialsByPhone(phone).orElseThrow();
        assertThat(byPhone.userId()).isEqualTo(created.id());
        assertThat(byPhone.passwordHash()).isEqualTo("$argon2id$fake");
        assertThat(byPhone.phoneVerified()).isFalse();
        assertThat(userService.findCredentialsByEmail(email.toLowerCase(java.util.Locale.ROOT))).isPresent();
        assertThat(userService.findCredentialsByPhone(uniquePhone())).isEmpty();
    }

    @Test
    void rejectsADuplicatePhoneAndADuplicateEmail() {
        String phone = uniquePhone();
        String email = uniqueEmail();
        userService.create(sample(phone, email, null));

        assertThatThrownBy(() -> userService.create(sample(phone, uniqueEmail(), null)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> userService.create(sample(uniquePhone(), email, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsManyUsersWithoutAnEmail() {
        userService.create(sample(uniquePhone(), null, null));
        userService.create(sample(uniquePhone(), null, null));
    }

    @Test
    void encryptsTheNationalIdAtRestAndDecryptsItOnRead() {
        UserView created = userService.create(sample(uniquePhone(), uniqueEmail(), "CM0099887766"));

        String stored = jdbcTemplate.queryForObject(
                "select national_id from users where id = ?", String.class, created.id());

        assertThat(stored).isNotNull();
        assertThat(stored).doesNotContain("CM0099887766");
        assertThat(stored).matches("^[A-Za-z0-9+/]+=*$");
        assertThat(stored.length()).isGreaterThan(20);
        assertThat(userService.nationalIdOf(created.id()).orElseThrow()).isEqualTo("CM0099887766");
    }

    @Test
    void marksThePhoneVerifiedOnceAndIsIdempotent() {
        UserView created = userService.create(sample(uniquePhone(), uniqueEmail(), null));

        userService.markPhoneVerified(created.id());
        UserView verified = userService.findById(created.id()).orElseThrow();
        assertThat(verified.phoneVerified()).isTrue();

        userService.markPhoneVerified(created.id());
        assertThat(userService.findById(created.id()).orElseThrow().phoneVerified()).isTrue();
    }

    @Test
    void defaultsAndValidatesCountryAndLanguage() {
        UserView congolese = userService.create(
                new NewUser(uniquePhone(), "$argon2id$fake", "Jean", "Kabila", "CD", "fr", null, null));

        assertThat(congolese.country()).isEqualTo("CD");
        assertThat(congolese.language()).isEqualTo("fr");
        assertThatThrownBy(() -> userService.create(
                new NewUser(uniquePhone(), "$argon2id$fake", "X", "Y", "KE", "en", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
