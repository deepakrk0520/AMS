package com.shopsphere.api.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Degraded-mode strategy: if Redis fails during a cache operation, log a
 * warning (cache name and exception type, never the key/value or connection
 * details) and carry on, so the underlying method still runs and the caller
 * gets a normal response. The application also starts without Redis.
 */
class CacheErrorLogger implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(CacheErrorLogger.class);

    @Override
    public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
        warn("get", cache, ex);
    }

    @Override
    public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
        warn("put", cache, ex);
    }

    @Override
    public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
        warn("evict", cache, ex);
    }

    @Override
    public void handleCacheClearError(RuntimeException ex, Cache cache) {
        warn("clear", cache, ex);
    }

    private void warn(String operation, Cache cache, RuntimeException ex) {
        log.warn("Redis cache {} failed for cache '{}' ({}); continuing without cache",
                operation, cache.getName(), ex.getClass().getSimpleName());
        log.debug("Cache error detail", ex);
    }
}
