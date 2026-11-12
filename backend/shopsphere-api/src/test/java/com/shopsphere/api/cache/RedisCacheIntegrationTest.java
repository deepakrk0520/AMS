package com.shopsphere.api.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

/**
 * Requires the docker-compose Redis (like ShopsphereApiApplicationTests needs PostgreSQL):
 * {@code docker compose up -d redis}. Loads only the cache slice, so no database is needed.
 * The "demo" cache TTL is shortened to 2s to exercise expiry.
 */
@SpringBootTest(classes = { CacheConfig.class, CacheDemoService.class }, properties = {
        "spring.data.redis.host=${SPRING_DATA_REDIS_HOST:localhost}", // CI points this at its Redis container
        "spring.data.redis.port=${REDIS_PORT:6379}",
        "app.cache.default-ttl=10m",
        "app.cache.ttls.demo=2s" })
@ImportAutoConfiguration(RedisAutoConfiguration.class)
class RedisCacheIntegrationTest {

    @Autowired
    private CacheDemoService service;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private RedisConnectionFactory connectionFactory;
    @Autowired
    private CacheManager cacheManager;

    private static String key() {
        return "it-" + UUID.randomUUID();
    }

    private String rawCacheKey(String key) {
        return CacheNames.DEMO + "::" + key;
    }

    @Test
    void connects() {
        try (var connection = connectionFactory.getConnection()) {
            assertThat(connection.ping()).isEqualTo("PONG");
        }
    }

    @Test
    void writeReadDelete() {
        String k = key();
        assertThat(service.retrieve(k)).isEmpty();
        service.store(k, "hello", Duration.ofMinutes(1));
        assertThat(service.retrieve(k)).contains("hello");
        assertThat(service.remove(k)).isTrue();
        assertThat(service.retrieve(k)).isEmpty();
        assertThat(service.remove(k)).isFalse();
    }

    @Test
    void directValueExpiresAfterTtl() {
        String k = key();
        service.store(k, "short-lived", Duration.ofSeconds(1));
        assertThat(service.retrieve(k)).isPresent();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(service.retrieve(k)).isEmpty());
    }

    @Test
    void missThenHit() {
        String k = key();
        int before = service.loadCount();
        DemoValue first = service.get(k);   // miss: method runs
        DemoValue second = service.get(k);  // hit: served from Redis
        assertThat(service.loadCount()).isEqualTo(before + 1);
        assertThat(second).isEqualTo(first);
        service.evict(k);
    }

    @Test
    void evictForcesReload() {
        String k = key();
        service.get(k);
        int afterFirstLoad = service.loadCount();
        service.evict(k);
        assertThat(redis.hasKey(rawCacheKey(k))).isFalse();
        service.get(k);
        assertThat(service.loadCount()).isEqualTo(afterFirstLoad + 1);
        service.evict(k);
    }

    @Test
    void cachePutRefreshesEntryWithoutRunningLoader() {
        String k = key();
        service.get(k);
        int loads = service.loadCount();
        DemoValue updated = service.update(k, "refreshed");
        DemoValue cached = service.get(k);
        assertThat(cached.value()).isEqualTo("refreshed");
        assertThat(cached).isEqualTo(updated);
        assertThat(service.loadCount()).isEqualTo(loads);
        service.evict(k);
    }

    @Test
    void cachedEntryExpiresAfterConfiguredTtl() {
        String k = key();
        service.get(k);
        Long ttl = redis.getExpire(rawCacheKey(k));
        assertThat(ttl).isBetween(1L, 2L); // demo TTL = 2s
        int loads = service.loadCount();
        await().atMost(Duration.ofSeconds(6)).untilAsserted(() -> assertThat(redis.hasKey(rawCacheKey(k))).isFalse());
        service.get(k);
        assertThat(service.loadCount()).isEqualTo(loads + 1);
        service.evict(k);
    }

    @Test
    void valuesAreStoredAsJsonAndRoundTripToTheirType() {
        String k = key();
        DemoValue original = service.get(k);
        String raw = redis.opsForValue().get(rawCacheKey(k));
        assertThat(raw).startsWith("{").contains("\"@class\":\"com.shopsphere.api.cache.DemoValue\"")
                .contains("\"value\":\"" + original.value() + "\"");
        // Java native serialization would start with the 0xACED stream header.
        assertThat(raw).doesNotStartWith("¬í");
        Object read = cacheManager.getCache(CacheNames.DEMO).get(k).get();
        assertThat(read).isInstanceOf(DemoValue.class).isEqualTo(original);
        service.evict(k);
    }

    @Test
    void defaultTtlAppliesToCachesWithoutOverride() {
        String k = key();
        cacheManager.getCache("some-other-cache").put(k, "v");
        Long ttl = redis.getExpire("some-other-cache::" + k);
        assertThat(ttl).isBetween(590L, 600L); // 10m default
        cacheManager.getCache("some-other-cache").evict(k);
    }
}
