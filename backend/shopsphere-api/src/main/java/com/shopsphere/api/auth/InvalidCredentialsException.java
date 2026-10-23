package com.shopsphere.api.auth;

/** Login failed; deliberately carries no detail about why. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials", null, false, false);
    }
}
