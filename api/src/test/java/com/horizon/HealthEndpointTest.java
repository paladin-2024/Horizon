package com.horizon;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthEndpointTest {

    @Value("${local.server.port}")
    int port;

    @Autowired
    SecurityFilterChain securityFilterChain;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthIsPublicAndUp() throws Exception {
        var response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void anyOtherPathRequiresAuthentication() throws Exception {
        assertThat(get("/api/v1/anything").statusCode()).isEqualTo(401);
    }

    @Test
    void actuatorMetricsRequireAuthentication() throws Exception {
        assertThat(get("/actuator/metrics").statusCode()).isEqualTo(401);
    }

    @Test
    void unauthenticatedPostIsRejectedWith401() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/anything"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void springCsrfFilterIsNotInTheChain() {
        // An unauthenticated POST already answers 401 because the CSRF 403 is re-dispatched to /error and
        // turned into a 401, so the status alone cannot show CSRF is off. Check the chain itself.
        assertThat(securityFilterChain.getFilters()).noneMatch(CsrfFilter.class::isInstance);
    }
}
