package com.shopsphere.api.exception;

import java.time.Instant;

/** Standard error body returned by all API endpoints. */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {
}
