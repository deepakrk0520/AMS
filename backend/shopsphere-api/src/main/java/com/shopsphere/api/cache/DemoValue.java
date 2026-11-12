package com.shopsphere.api.cache;

import java.time.Instant;

/** Sample cached value used by the demonstration only. */
public record DemoValue(String key, String value, Instant createdAt) {
}
