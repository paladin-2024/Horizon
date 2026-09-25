package com.horizon.support;

import com.horizon.common.audit.AuditLog;
import com.horizon.common.error.ApiException;
import com.horizon.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoints for exercising the common module end to end. It is a {@code @TestComponent},
 * so component scanning skips it; tests opt in with {@code @Import(ProbeController.class)}.
 * The error, validation and request-id endpoints are used by the tests of Tasks 5 and 6.
 */
@RestController
@TestComponent
@RequestMapping("/test-support")
public class ProbeController {

    private static final Logger log = LoggerFactory.getLogger(ProbeController.class);

    private final AuditLog auditLog;

    public ProbeController(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    public record ProbeBody(@NotBlank String name, @Min(1) int quantity) {
    }

    @GetMapping("/whoami")
    Map<String, Object> whoami() {
        return Map.of("userId", CurrentUser.id().toString());
    }

    @PostMapping("/echo")
    Map<String, Object> echo() {
        return Map.of("ok", true);
    }

    @GetMapping("/api-error")
    void apiError(@RequestParam int status, @RequestParam String code) {
        throw new ApiException(HttpStatus.valueOf(status), code, "probe message for " + code);
    }

    @PostMapping("/validate")
    Map<String, Object> validate(@Valid @RequestBody ProbeBody body) {
        return Map.of("name", body.name());
    }

    @GetMapping("/limit")
    Map<String, Object> limit(@RequestParam @Min(1) @Max(100) int limit) {
        return Map.of("limit", limit);
    }

    @GetMapping("/boom")
    void boom() {
        throw new IllegalStateException("secret internal detail");
    }

    @GetMapping("/stale")
    void stale(@RequestParam(defaultValue = "plain") String kind) {
        if ("object".equals(kind)) {
            throw new ObjectOptimisticLockingFailureException(Object.class, "id");
        }
        throw new OptimisticLockingFailureException("row changed");
    }

    @PostMapping("/audit")
    Map<String, Object> audit() {
        auditLog.record(CurrentUser.id(), "probe.audit", Map.of("source", "probe"));
        return Map.of("ok", true);
    }

    @GetMapping("/request-id")
    Map<String, Object> requestId() {
        log.info("probe request-id endpoint called");
        return Map.of("requestId", String.valueOf(MDC.get("requestId")));
    }
}
