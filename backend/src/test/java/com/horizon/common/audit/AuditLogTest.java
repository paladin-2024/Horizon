package com.horizon.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.horizon.support.IntegrationTest;
import com.horizon.support.ProbeController;
import com.horizon.support.TestAuth;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;

@IntegrationTest
@Import(ProbeController.class)
class AuditLogTest {

    @Autowired
    AuditLog auditLog;

    @Autowired
    AuditLogRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    MockMvc mockMvc;

    @Test
    void recordStoresUserActionAndMetadata() {
        UUID userId = UUID.randomUUID();

        auditLog.record(userId, "auth.login", Map.of("method", "password", "attempt", 1));

        List<AuditLogEntry> rows = repository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(rows).hasSize(1);
        AuditLogEntry row = rows.get(0);
        assertThat(row.getAction()).isEqualTo("auth.login");
        assertThat(row.getMetadata()).containsEntry("method", "password").containsEntry("attempt", 1);
        assertThat(row.getId().version()).isEqualTo(7);
        assertThat(row.getCreatedAt()).isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
    }

    @Test
    void nullMetadataIsStoredAsAnEmptyObject() {
        UUID userId = UUID.randomUUID();

        auditLog.record(userId, "auth.logout", null);

        String stored = jdbc.queryForObject(
                "select metadata::text from audit_log where user_id = ?", String.class, userId);
        assertThat(stored).isEqualTo("{}");
    }

    @Test
    void unauthenticatedActionsAreStoredWithoutAUser() {
        String action = "auth.login_failed." + UUID.randomUUID();

        auditLog.record(null, action, Map.of("identifier", "unknown"));

        Integer count = jdbc.queryForObject(
                "select count(*) from audit_log where user_id is null and action = ?", Integer.class, action);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void ipIsNullWhenThereIsNoCurrentRequest() {
        UUID userId = UUID.randomUUID();
        // @SpringBootTest binds a mock request to the test thread; clear it to simulate a job.
        RequestContextHolder.resetRequestAttributes();

        auditLog.record(userId, "system.job", Map.of());

        assertThat(repository.findByUserIdOrderByCreatedAtDesc(userId).get(0).getIp()).isNull();
    }

    @Test
    void ipComesFromTheCurrentRequest() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/test-support/audit")
                        .with(TestAuth.asUser(userId))
                        .with(request -> {
                            request.setRemoteAddr("10.1.2.3");
                            return request;
                        }))
                .andExpect(status().isOk());

        List<AuditLogEntry> rows = repository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getAction()).isEqualTo("probe.audit");
        assertThat(rows.get(0).getIp()).isEqualTo("10.1.2.3");
    }

    @Test
    void rowJoinsTheCallersTransactionAndRollsBackWithIt() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            auditLog.record(userId, "account.link", Map.of());
            throw new IllegalStateException("business failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(repository.findByUserIdOrderByCreatedAtDesc(userId)).isEmpty();
    }

    @Test
    void metadataColumnIsJsonb() {
        String type = jdbc.queryForObject(
                "select data_type from information_schema.columns "
                        + "where table_name = 'audit_log' and column_name = 'metadata'",
                String.class);

        assertThat(type).isEqualTo("jsonb");
    }

    @Test
    void userAndCreatedAtAreIndexedTogether() {
        String definition = jdbc.queryForObject(
                "select indexdef from pg_indexes where tablename = 'audit_log' "
                        + "and indexname = 'idx_audit_log_user_created'",
                String.class);

        assertThat(definition).contains("(user_id, created_at)");
    }
}
