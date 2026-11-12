package com.shopsphere.api.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Explicit, greppable start/stop markers for incident timelines. */
@Component
class LifecycleLogger {

    private static final Logger log = LoggerFactory.getLogger(LifecycleLogger.class);

    private final Environment environment;

    LifecycleLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener
    void onReady(ApplicationReadyEvent event) {
        log.info("ShopSphere API started: port={}, profiles={}", environment.getProperty("local.server.port",
                environment.getProperty("server.port", "8080")), java.util.Arrays.toString(
                        environment.getActiveProfiles().length == 0 ? new String[] { "default" }
                                : environment.getActiveProfiles()));
    }

    @EventListener
    void onClosing(ContextClosedEvent event) {
        log.info("ShopSphere API shutting down");
    }
}
