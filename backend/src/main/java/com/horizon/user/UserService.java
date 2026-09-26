package com.horizon.user;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The public API of the user module. */
@Service
public class UserService {

    private final UserRepository repository;

    UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<UserView> findById(UUID id) {
        return repository.findById(id).map(UserService::toView);
    }

    /**
     * Creates a user with an unverified phone.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException if the phone or email is taken
     * @throws IllegalArgumentException if the country or language is not supported
     */
    @Transactional
    public UserView create(NewUser command) {
        Country country = Country.valueOf(command.country());
        Language language = Language.fromCode(command.language());
        User user = new User(
                command.phone(),
                command.passwordHash(),
                command.firstName(),
                command.lastName(),
                command.nationalId(),
                country,
                language,
                normalizeEmail(command.email()),
                Instant.now());
        return toView(repository.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public Optional<UserCredentials> findCredentialsByPhone(String phone) {
        return repository.findByPhone(phone).map(UserService::toCredentials);
    }

    @Transactional(readOnly = true)
    public Optional<UserCredentials> findCredentialsByEmail(String email) {
        return repository.findByEmail(normalizeEmail(email)).map(UserService::toCredentials);
    }

    @Transactional
    public void markPhoneVerified(UUID userId) {
        repository.findById(userId).ifPresent(user -> user.markPhoneVerified(Instant.now()));
    }

    /** Only for tests and future support tooling: the decrypted national ID. */
    @Transactional(readOnly = true)
    public Optional<String> nationalIdOf(UUID userId) {
        return repository.findById(userId).map(User::getNationalId);
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UserView toView(User user) {
        return new UserView(user.getId(), user.getPhone(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getCountry().name(), user.getLanguage().code(),
                user.isPhoneVerified());
    }

    private static UserCredentials toCredentials(User user) {
        return new UserCredentials(user.getId(), user.getPhone(), user.getPasswordHash(),
                user.isPhoneVerified());
    }
}
