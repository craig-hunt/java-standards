package com.sagecrest.standards.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Asserts that the pool is configured the way the service depends on.
 *
 * <p>{@link PostgresFixture} already opens a pool, but from a static initializer that runs once per
 * JVM, so nothing could tell whether a later edit broke the configuration: every setting could be
 * dropped and the suite would still pass. Opening a pool inside a test method is what makes the
 * settings verifiable.
 *
 * <p>The lock timeout is the setting this exists for. Without a test, a line that applies it is
 * indistinguishable from a line that does not, and the symptom in production is a request that
 * never answers and never logs.
 */
class ConnectionPoolIT extends PostgresFixture {

  private static final String SQL_SHOW_LOCK_TIMEOUT = "SHOW lock_timeout";
  private static final String SQL_WHO_AM_I = "SELECT current_user";
  private static final String EXPECTED_LOCK_TIMEOUT = "2s";
  private static final String WRONG_PASSWORD = "not-the-password";
  private static final String AUTHENTICATION_FAILED = "password authentication failed";
  private static final int FIRST_COLUMN = 1;

  private static final PostgreSQLContainer<?> POSTGRES = container();

  private static HikariDataSource open(String password) {
    return ConnectionPool.open(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), password);
  }

  private static String readOne(HikariDataSource pool, String sql) throws Exception {
    try (var connection = pool.getConnection();
        PreparedStatement query = connection.prepareStatement(sql);
        ResultSet rows = query.executeQuery()) {
      rows.next();
      return rows.getString(FIRST_COLUMN);
    }
  }

  @Test
  @DisplayName("bounds how long a statement waits for a lock, rather than waiting forever")
  void boundsLockWaits() throws Exception {
    try (HikariDataSource pool = open(POSTGRES.getPassword())) {
      assertThat(readOne(pool, SQL_SHOW_LOCK_TIMEOUT))
          .as("a connection that never applied the setting reports 0, meaning no limit")
          .isEqualTo(EXPECTED_LOCK_TIMEOUT);
    }
  }

  @Test
  @DisplayName("connects as the user it was given")
  void connectsAsTheGivenUser() throws Exception {
    try (HikariDataSource pool = open(POSTGRES.getPassword())) {
      assertThat(readOne(pool, SQL_WHO_AM_I)).isEqualTo(POSTGRES.getUsername());
    }
  }

  @Test
  @DisplayName("fails while opening on a bad password, so the service dies at startup")
  void failsWhileOpeningOnABadPassword() {
    // The pool authenticates as it initializes rather than on first use, which is the
    // behavior worth having: a wrong password becomes a deployment that never goes live
    // instead of a service that answers health checks and fails every real request. It
    // also proves the password argument is the one actually presented.
    assertThatThrownBy(() -> open(WRONG_PASSWORD))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining(AUTHENTICATION_FAILED)
        .hasMessageNotContainingAny(WRONG_PASSWORD);
  }
}
