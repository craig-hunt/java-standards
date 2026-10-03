package com.sagecrest.standards.infrastructure.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Opens the pool, in the one place that knows how.
 *
 * <p>Both entry points need a pool, and two copies of this would eventually disagree about a
 * setting that matters. It takes three strings rather than a settings type, so this module stays
 * ignorant of where configuration comes from.
 */
public final class ConnectionPool {

  /**
   * Bounds how long a statement waits for a row lock before giving up.
   *
   * <p>PostgreSQL waits forever by default, which turns contention into a request that never
   * answers and never errors: no log line, no metric, nothing for an operator to act on. The task
   * store takes a row lock to read a completion flag before changing it, so this is reachable
   * whenever two requests complete the same task at once.
   *
   * <p>It bounds the wait for a lock only, not the time a statement takes to run. A query that is
   * slow because it is doing work is a different problem with a different answer.
   */
  private static final String SQL_BOUND_LOCK_WAITS = "SET lock_timeout = '2s'";

  public static HikariDataSource open(String url, String user, String password) {
    HikariConfig configured = new HikariConfig();
    configured.setJdbcUrl(url);
    configured.setUsername(user);
    configured.setPassword(password);
    configured.setConnectionInitSql(SQL_BOUND_LOCK_WAITS);
    return new HikariDataSource(configured);
  }

  private ConnectionPool() {}
}
