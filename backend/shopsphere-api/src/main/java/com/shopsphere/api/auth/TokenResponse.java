package com.shopsphere.api.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
