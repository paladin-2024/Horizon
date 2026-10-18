package com.horizon.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findFirstByPhoneAndPurposeAndConsumedAtIsNullOrderByIdDesc(String phone,
            OtpPurpose purpose);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OtpCode o set o.consumedAt = :now "
            + "where o.phone = :phone and o.purpose = :purpose and o.consumedAt is null")
    int consumeAllFor(@Param("phone") String phone, @Param("purpose") OtpPurpose purpose,
            @Param("now") Instant now);

    @Modifying
    @Query(value = "delete from otp_codes where id in "
            + "(select id from otp_codes where expires_at < :cutoff limit :batchSize)",
            nativeQuery = true)
    int deleteExpiredBatch(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);
}
