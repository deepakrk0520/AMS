package com.shopsphere.api.auth;

import java.util.UUID;

import com.shopsphere.api.security.Role;
import com.shopsphere.api.user.User;

/** Public view of a user. Never includes the password or its hash. */
public record UserResponse(UUID id, String firstName, String lastName, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getRole());
    }
}
