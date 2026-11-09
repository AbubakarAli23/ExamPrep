package com.examprep.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Starts one real PostgreSQL server for the whole test JVM. Tests exercise the actual
 * Flyway migrations and PostgreSQL JSONB behaviour without needing Docker.
 */
public final class PostgresSupport {

    private static volatile EmbeddedPostgres instance;

    private PostgresSupport() {
    }

    public static EmbeddedPostgres get() {
        EmbeddedPostgres local = instance;
        if (local == null) {
            synchronized (PostgresSupport.class) {
                local = instance;
                if (local == null) {
                    try {
                        local = EmbeddedPostgres.builder().start();
                    } catch (IOException ex) {
                        throw new UncheckedIOException("Could not start embedded PostgreSQL", ex);
                    }
                    instance = local;
                }
            }
        }
        return local;
    }
}
