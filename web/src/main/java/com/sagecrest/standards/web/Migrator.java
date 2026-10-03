package com.sagecrest.standards.web;

import com.sagecrest.standards.infrastructure.persistence.ConnectionPool;
import com.sagecrest.standards.infrastructure.persistence.Database;
import com.sagecrest.standards.infrastructure.persistence.Schema;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the schema and exits.
 *
 * <p>A separate process on purpose. If the API applied the schema at startup, two replicas rolling
 * out together would run the same DDL at the same moment, and PostgreSQL answers concurrent DDL on
 * one table with a lock wait or a deadlock rather than with a tidy no-op. Running migration to
 * completion before any replica starts removes the race instead of hoping to win it.
 *
 * <p>It reads only the database settings, so it cannot be blocked by a missing API token it would
 * never present.
 */
public final class Migrator {

  private static final Logger LOG = LoggerFactory.getLogger(Migrator.class);

  private static final String MSG_APPLIED = "schema applied";

  public static void main(String[] args) {
    DatabaseSettings settings = DatabaseSettings.fromEnvironment();
    try (HikariDataSource pool =
        ConnectionPool.open(settings.url(), settings.user(), settings.password())) {
      new Schema(new Database(pool)).apply();
    }
    LOG.info(MSG_APPLIED);
  }

  private Migrator() {}
}
