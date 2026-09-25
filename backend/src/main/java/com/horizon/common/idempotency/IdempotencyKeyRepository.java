package com.horizon.common.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByScopeIdAndKey(UUID scopeId, String key);

    /**
     * Claims an in-flight row whose lock has expired. Conditional on the status and the old lock, so
     * two requests racing to take over the same stale key cannot both win.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            update IdempotencyKey k
               set k.lockedUntil = :newLockedUntil
             where k.id = :id
               and k.status = :status
               and k.lockedUntil <= :now
            """)
    int takeOverStaleLock(
            @Param("id") UUID id,
            @Param("status") IdempotencyStatus status,
            @Param("now") Instant now,
            @Param("newLockedUntil") Instant newLockedUntil);

    /** Deletes at most {@code batchSize} expired rows; returns how many were deleted. */
    @Modifying
    @Query(
            value = """
                    delete from idempotency_keys
                     where id in (select id from idempotency_keys
                                   where expires_at <= :now
                                   order by expires_at
                                   limit :batchSize)
                    """,
            nativeQuery = true)
    int deleteExpiredBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
