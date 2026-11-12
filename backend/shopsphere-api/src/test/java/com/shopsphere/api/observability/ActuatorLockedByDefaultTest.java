package com.shopsphere.api.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

/** With no ACTUATOR_PASSWORD configured, nobody can read metrics (secure by default). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.jwt.secret=test-only-secret-test-only-secret-0123456789",
        "app.security.actuator.password=" })
@AutoConfigureObservability
class ActuatorLockedByDefaultTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void metricsRejectEveryoneWhenNoPasswordIsConfigured() {
        assertThat(rest.getForEntity("/actuator/prometheus", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.withBasicAuth("prometheus", "").getForEntity("/actuator/prometheus", String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.getForEntity("/actuator/health", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
