package com.horizon.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.horizon.support.IdempotencyTestController;
import com.horizon.support.ResilienceTestSecurity;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({ResilienceTestSecurity.class, IdempotencyTestController.class})
class IdempotencyConcurrencyTest {

    @Value("${local.server.port}")
    int port;

    @Autowired
    IdempotencyTestController controller;

    @Autowired
    IdempotencyKeyRepository repository;

    @Autowired
    IdempotencyProperties properties;

    @BeforeEach
    void resetCounter() {
        controller.reset();
    }

    private HttpRequest echo(String key, String body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/test/echo"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", key)
                .header("X-Horizon-Client", "web")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    @Test
    void twoSimultaneousRequestsRunTheHandlerOnceAndTheLoserGets409() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = "{\"value\":\"race\",\"delayMs\":700,\"fail\":false}";
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<HttpResponse<String>> call = () -> {
            HttpClient client = HttpClient.newHttpClient(); // separate clients: two real connections
            start.await();
            return client.send(echo(key, body), HttpResponse.BodyHandlers.ofString());
        };

        Future<HttpResponse<String>> first = pool.submit(call);
        Future<HttpResponse<String>> second = pool.submit(call);
        start.countDown();
        List<HttpResponse<String>> responses =
                List.of(first.get(60, TimeUnit.SECONDS), second.get(60, TimeUnit.SECONDS));
        pool.shutdown();

        List<Integer> statuses = responses.stream()
                .map(HttpResponse::statusCode)
                .sorted(Comparator.naturalOrder())
                .toList();
        assertThat(statuses).containsExactly(201, 409);
        assertThat(controller.invocations()).isEqualTo(1);

        HttpResponse<String> conflict = responses.stream()
                .filter(response -> response.statusCode() == 409)
                .findFirst()
                .orElseThrow();
        assertThat(conflict.body()).contains("\"code\":\"idempotency_in_flight\"");

        HttpResponse<String> replay = HttpClient.newHttpClient().send(echo(key, body), HttpResponse.BodyHandlers.ofString());
        assertThat(replay.statusCode()).isEqualTo(201);
        assertThat(replay.headers().firstValue("Idempotency-Replayed")).contains("true");
        assertThat(replay.body()).isEqualTo(responses.stream()
                .filter(response -> response.statusCode() == 201)
                .findFirst()
                .orElseThrow()
                .body());
        assertThat(controller.invocations()).isEqualTo(1);
    }

    @Test
    void aCrashedInFlightRequestDoesNotBlockTheKeyForever() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = "{\"value\":\"crashed\",\"delayMs\":0,\"fail\":false}";
        HttpClient client = HttpClient.newHttpClient();
        assertThat(client.send(echo(key, body), HttpResponse.BodyHandlers.ofString()).statusCode())
                .isEqualTo(201);

        // Simulate a process that died mid-request: the row is back to IN_PROGRESS with an expired lock.
        simulateCrashedRequest(key);

        HttpResponse<String> retry = client.send(echo(key, body), HttpResponse.BodyHandlers.ofString());

        assertThat(retry.statusCode()).isEqualTo(201);
        assertThat(retry.headers().firstValue("Idempotency-Replayed")).isEmpty();
        assertThat(controller.invocations()).isEqualTo(2);
        IdempotencyKey stored = repository.findByScopeIdAndKey(IdempotencyKey.ANONYMOUS_SCOPE, key).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
    }

    private void simulateCrashedRequest(String key) {
        UUID id = repository
                .findByScopeIdAndKey(IdempotencyKey.ANONYMOUS_SCOPE, key)
                .orElseThrow()
                .getId();
        repository.markInProgressWithExpiredLock(id, Instant.now().minus(properties.lockTimeout()));
    }
}
