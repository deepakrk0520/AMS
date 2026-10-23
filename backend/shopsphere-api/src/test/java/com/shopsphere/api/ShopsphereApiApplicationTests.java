package com.shopsphere.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Requires the local PostgreSQL from docker-compose.yml to be running. */
@SpringBootTest(properties = "app.security.jwt.secret=test-only-secret-test-only-secret-0123456789")
class ShopsphereApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
