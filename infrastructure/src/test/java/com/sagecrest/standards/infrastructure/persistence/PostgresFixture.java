package com.sagecrest.standards.infrastructure.persistence;

import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One PostgreSQL container for the whole run, with the schema already applied.
 *
 * <p>Started once in a static block rather than per test class. Testcontainers' own
 * {@code @Container} annotation ties the lifetime to one class, which would start and stop a
 * database for each of these, and a reference suite that takes a minute to run is a suite people
 * skip.
 *
 * <p>A real PostgreSQL rather than an in-memory substitute. Everything worth testing here is
 * PostgreSQL-specific: {@code RETURNING}, {@code FOR UPDATE SKIP LOCKED}, {@code jsonb}, and a
 * partial index. A fake would agree with the code and disagree with production.
 */
public abstract class PostgresFixture {

  private static final String IMAGE = "postgres:16-alpine";

  private static final String SQL_TRUNCATE =
      "TRUNCATE tasks, signups, outbox RESTART IDENTITY CASCADE";

  private static final Database DATABASE;

  static {
    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(IMAGE);
    postgres.start();

    // The same call the migrator makes. A fixture that built its own pool would
    // verify a configuration no deployment uses, and would leave the real one
    // with no test at all.
    DATABASE =
        new Database(
            ConnectionPool.open(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
    new Schema(DATABASE).apply();
  }

  protected static Database database() {
    return DATABASE;
  }

  /**
   * Leaves each test a database holding the seed rows and nothing else.
   *
   * <p>Inventory is deliberately not truncated: the seed is what the stock tests read, and
   * re-seeding per test would make each one pay for six inserts to reach the state the schema
   * already guarantees.
   */
  @BeforeEach
  void clearTables() {
    DATABASE.inTransaction(
        connection -> {
          try (Statement truncate = connection.createStatement()) {
            truncate.execute(SQL_TRUNCATE);
          }
          return null;
        });
  }
}
