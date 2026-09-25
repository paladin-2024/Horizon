package com.horizon.support;

import com.horizon.common.error.ApiException;
import com.horizon.common.idempotency.Idempotent;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Fake idempotent endpoint. The invocation counter proves the handler ran exactly once. */
@RestController
@RequestMapping("/api/v1/test")
public class IdempotencyTestController {

    public record EchoRequest(String value, long delayMs, boolean fail) {}

    public record EchoResponse(String echo, int invocation) {}

    public record UploadResponse(String name, long size, int invocation) {}

    private final AtomicInteger invocations = new AtomicInteger();

    @Idempotent
    @PostMapping("/echo")
    public ResponseEntity<EchoResponse> echo(@RequestBody EchoRequest request) throws InterruptedException {
        int invocation = invocations.incrementAndGet();
        if (request.delayMs() > 0) {
            Thread.sleep(request.delayMs());
        }
        if (request.fail()) {
            throw ApiException.badRequest("test_failure", "The test handler was asked to fail.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(new EchoResponse(request.value(), invocation));
    }

    /** Multipart endpoint: proves the request hash covers the uploaded file, not just its length. */
    @Idempotent
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        int invocation = invocations.incrementAndGet();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UploadResponse(file.getOriginalFilename(), file.getSize(), invocation));
    }

    public int invocations() {
        return invocations.get();
    }

    public void reset() {
        invocations.set(0);
    }
}
