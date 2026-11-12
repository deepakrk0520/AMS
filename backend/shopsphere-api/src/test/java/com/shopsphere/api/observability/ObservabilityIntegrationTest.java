package com.shopsphere.api.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Needs the docker-compose PostgreSQL and Redis (like ShopsphereApiApplicationTests). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.jwt.secret=test-only-secret-test-only-secret-0123456789",
        "app.security.actuator.username=scraper",
        "app.security.actuator.password=scrape-pw-for-tests" })
@AutoConfigureObservability // Boot disables metrics export in tests by default
class ObservabilityIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private TestRestTemplate scraper() {
        return rest.withBasicAuth("scraper", "scrape-pw-for-tests");
    }

    @Test
    @SuppressWarnings("unchecked")
    void healthReportsDatabaseAndRedisWithoutDetails() {
        ResponseEntity<Map> response = rest.getForEntity("/actuator/health", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsEntry("status", "UP");
        Map<String, Map<String, Object>> components = (Map<String, Map<String, Object>>) body.get("components");
        assertThat(components).containsKeys("db", "redis");
        assertThat(components.get("db")).containsEntry("status", "UP").doesNotContainKey("details");
        assertThat(components.get("redis")).containsEntry("status", "UP").doesNotContainKey("details");
    }

    @Test
    void infoIsPublicAndShowsBuildInfo() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/info", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("shopsphere-api");
    }

    @Test
    void metricsAndPrometheusRequireCredentials() {
        for (String path : new String[] { "/actuator/prometheus", "/actuator/metrics" }) {
            assertThat(rest.getForEntity(path, String.class).getStatusCode())
                    .as(path + " anonymous").isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(rest.withBasicAuth("scraper", "wrong").getForEntity(path, String.class).getStatusCode())
                    .as(path + " wrong password").isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(scraper().getForEntity(path, String.class).getStatusCode())
                    .as(path + " valid credentials").isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void sensitiveActuatorEndpointsAreNotExposed() {
        for (String path : new String[] { "/actuator/env", "/actuator/beans", "/actuator/heapdump",
                "/actuator/configprops", "/actuator/loggers" }) {
            assertThat(scraper().getForEntity(path, String.class).getStatusCode().is2xxSuccessful())
                    .as(path).isFalse();
            assertThat(rest.getForEntity(path, String.class).getStatusCode().is2xxSuccessful())
                    .as(path + " anonymous").isFalse();
        }
    }

    @Test
    void prometheusExposesJvmDatabaseRedisCacheAndHttpMetrics() {
        // generate HTTP traffic: one success and one client error
        rest.getForEntity("/api/v1/health", String.class);
        rest.getForEntity("/api/v1/auth/me", String.class);

        String metrics = scraper().getForEntity("/actuator/prometheus", String.class).getBody();
        assertThat(metrics)
                .contains("jvm_memory_used_bytes{").contains("area=\"heap\"")
                .contains("area=\"nonheap\"")
                .contains("jvm_gc_max_data_size_bytes") // jvm_gc_pause_seconds appears after the first GC
                .contains("jvm_threads_live_threads")
                .contains("hikaricp_connections_active")
                .contains("hikaricp_connections_idle")
                .contains("hikaricp_connections_acquire_seconds")
                .contains("http_server_requests_seconds_count")
                .contains("status=\"401\"")
                .contains("lettuce_command_completion_seconds")
                .contains("application=\"shopsphere-api\"");
    }

    @Test
    void everyResponseCarriesARequestId() {
        String generated = rest.getForEntity("/api/v1/health", String.class).getHeaders()
                .getFirst(RequestLoggingFilter.REQUEST_ID_HEADER);
        assertThat(generated).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.set(RequestLoggingFilter.REQUEST_ID_HEADER, "trace-me-123");
        String echoed = rest.exchange("/api/v1/health", org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), String.class).getHeaders().getFirst(RequestLoggingFilter.REQUEST_ID_HEADER);
        assertThat(echoed).isEqualTo("trace-me-123");

        headers.set(RequestLoggingFilter.REQUEST_ID_HEADER, "bad id with spaces\tand junk");
        String replaced = rest.exchange("/api/v1/health", org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), String.class).getHeaders().getFirst(RequestLoggingFilter.REQUEST_ID_HEADER);
        assertThat(replaced).doesNotContain(" ").isNotEqualTo("bad id with spaces\tand junk");

        // 401 responses (rejected by Spring Security) also get the id
        assertThat(rest.getForEntity("/api/v1/auth/me", String.class).getHeaders()
                .getFirst(RequestLoggingFilter.REQUEST_ID_HEADER)).isNotBlank();
    }
}
