package com.shopsphere.api.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

/** Redis unreachable (nothing listens on port 1): the app still works, uncached. Needs no Redis. */
@SpringBootTest(classes = { CacheConfig.class, CacheDemoService.class }, properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=1",
        "spring.data.redis.timeout=500ms",
        "spring.data.redis.connect-timeout=500ms",
        "app.cache.default-ttl=10m" })
@ImportAutoConfiguration(RedisAutoConfiguration.class)
class CacheDegradedModeTest {

    @Autowired
    private CacheDemoService service;

    @Test
    void cacheFailuresFallThroughToTheRealMethod() {
        int before = service.loadCount();
        DemoValue value = service.get("k");
        assertThat(value.value()).startsWith("loaded-");
        service.get("k"); // nothing cached, so it loads again instead of failing
        assertThat(service.loadCount()).isEqualTo(before + 2);
        assertThatCode(() -> service.evict("k")).doesNotThrowAnyException();
        assertThatCode(() -> service.update("k", "v")).doesNotThrowAnyException();
    }
}
