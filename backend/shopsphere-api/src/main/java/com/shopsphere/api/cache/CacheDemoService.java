package com.shopsphere.api.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * P1-T08 demonstration of the Redis integration. Not wired to any business
 * data or endpoint.
 *
 * <ul>
 *   <li>Direct operations ({@link #store}, {@link #retrieve}, {@link #remove}) use
 *       {@link StringRedisTemplate} under the {@code demo-direct:} key prefix.</li>
 *   <li>Annotation-driven operations use the {@code demo} cache.</li>
 * </ul>
 */
@Service
public class CacheDemoService {

    static final String DIRECT_PREFIX = "demo-direct:";

    private final StringRedisTemplate redis;
    private final AtomicInteger loads = new AtomicInteger();

    public CacheDemoService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void store(String key, String value, Duration ttl) {
        redis.opsForValue().set(DIRECT_PREFIX + key, value, ttl);
    }

    public Optional<String> retrieve(String key) {
        return Optional.ofNullable(redis.opsForValue().get(DIRECT_PREFIX + key));
    }

    public boolean remove(String key) {
        return Boolean.TRUE.equals(redis.delete(DIRECT_PREFIX + key));
    }

    /** Cache miss runs the method (counted by {@link #loadCount()}); a hit does not. */
    @Cacheable(cacheNames = CacheNames.DEMO, key = "#key")
    public DemoValue get(String key) {
        loads.incrementAndGet();
        return new DemoValue(key, "loaded-" + loads.get(), Instant.now());
    }

    /** Always runs and refreshes the cached entry with the returned value. */
    @CachePut(cacheNames = CacheNames.DEMO, key = "#key")
    public DemoValue update(String key, String value) {
        return new DemoValue(key, value, Instant.now());
    }

    @CacheEvict(cacheNames = CacheNames.DEMO, key = "#key")
    public void evict(String key) {
    }

    public int loadCount() {
        return loads.get();
    }
}
