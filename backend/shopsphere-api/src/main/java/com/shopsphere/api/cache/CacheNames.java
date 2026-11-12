package com.shopsphere.api.cache;

/** Central list of cache names. Add new names here and give them a TTL in application.properties if needed. */
public final class CacheNames {

    /** Used only by the P1-T08 demonstration; not tied to any business data. */
    public static final String DEMO = "demo";

    private CacheNames() {
    }
}
