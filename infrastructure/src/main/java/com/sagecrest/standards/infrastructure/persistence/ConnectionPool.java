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

  public static HikariDataSource open(String url, String user, String password) {
    HikariConfig configured = new HikariConfig();
    configured.setJdbcUrl(url);
    configured.setUsername(user);
    configured.setPassword(password);
    return new HikariDataSource(configured);
  }

  private ConnectionPool() {}
}
