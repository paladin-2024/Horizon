package com.horizon.common.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    List<AuditLogEntry> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
