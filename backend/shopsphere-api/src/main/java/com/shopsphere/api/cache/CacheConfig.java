package com.shopsphere.api.cache;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis-backed Spring Cache. Keys are plain strings ({@code cacheName::key});
 * values are stored as JSON (never Java native serialization). Null values are
 * not cached.
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig implements CachingConfigurer {

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, CacheProperties properties) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.defaultTtl())
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer()));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        properties.ttls().forEach((name, ttl) -> perCache.put(name, defaults.entryTtl(ttl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .enableStatistics() // cache hit/miss metrics (cache.gets, ...)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    /**
     * Cache failures must not break requests: log and fall through to the real method (degraded mode).
     * See {@link CacheErrorLogger}.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorLogger();
    }

    /**
     * JSON with a class-name type hint so values deserialize back to their original type.
     * Only classes under {@code com.shopsphere.api} and basic JDK value types may be
     * instantiated on read, which prevents polymorphic-deserialization gadget attacks.
     */
    @SuppressWarnings("deprecation") // EVERYTHING is needed so final classes (records) carry a type hint
    static RedisSerializer<Object> jsonSerializer() {
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.shopsphere.api.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.math.")
                .build();
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setDefaultTyping(new ObjectMapper.DefaultTypeResolverBuilder(
                ObjectMapper.DefaultTyping.EVERYTHING, validator)
                .init(JsonTypeInfo.Id.CLASS, null)
                .inclusion(JsonTypeInfo.As.PROPERTY)
                .typeProperty("@class"));
        return GenericJackson2JsonRedisSerializer.builder().objectMapper(mapper).build();
    }
}
