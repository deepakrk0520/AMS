package com.shopsphere.api.auth;

/** Registration attempted with an email that already exists. */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("Email is already registered", null, false, false);
    }
}
