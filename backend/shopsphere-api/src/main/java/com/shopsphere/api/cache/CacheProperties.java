package com.shopsphere.api.cache;

import java.time.Duration;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cache TTLs. {@code defaultTtl} applies to every cache unless {@code ttls}
 * has an entry for that cache name (e.g. {@code app.cache.ttls.demo=5m}).
 */
@Validated
@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(@NotNull Duration defaultTtl, Map<String, Duration> ttls) {

    public CacheProperties {
        ttls = ttls == null ? Map.of() : Map.copyOf(ttls);
    }
}
