package com.horizon.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface LinkedAccountRepository extends JpaRepository<LinkedAccount, UUID> {

    List<LinkedAccount> findByUserIdOrderByCreatedAtAscIdAsc(UUID userId);

    /** The ownership filter: a foreign account is indistinguishable from a missing one. */
    Optional<LinkedAccount> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndInstitutionIdAndAccountMask(UUID userId, UUID institutionId, String accountMask);
}
