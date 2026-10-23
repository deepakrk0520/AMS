package com.shopsphere.api.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings. The secret has no default and must be supplied via the
 * environment ({@code JWT_SECRET}); the application fails to start without
 * a sufficiently long one (HS256 needs at least 256 bits).
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "must be at least 32 characters") String secret,
        @NotBlank String issuer,
        @NotNull Duration expiration) {

    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", expiration=" + expiration + "]";
    }
}
