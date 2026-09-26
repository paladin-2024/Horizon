package com.horizon.auth;

import com.horizon.common.purge.DataPurger;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Deletes OTP codes whose expiry has passed; consumed codes expire on their own schedule. */
@Component
class OtpPurger implements DataPurger {

    private final OtpCodeRepository repository;

    OtpPurger(OtpCodeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() {
        return "otp_codes";
    }

    @Override
    @Transactional
    public int purgeBatch(int batchSize) {
        return repository.deleteExpiredBatch(Instant.now(), batchSize);
    }
}
