package com.sagecrest.standards.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sagecrest.standards.domain.inventory.InventorySeed;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts the claim the schema script makes about itself: that running it again changes nothing.
 *
 * <p>{@link PostgresFixture} already applies the schema, but it does so from a static initializer,
 * which runs once per JVM. Code reached only from there cannot be exercised a second time, so
 * nothing could tell whether a later change broke it. Calling {@code apply} from a test method is
 * both the coverage and the point: a reference service that refused to start twice would make every
 * local experiment begin by dropping a database.
 */
class SchemaIT extends PostgresFixture {

  private static final String SQL_COUNT_INVENTORY = "SELECT count(*) FROM inventory";
  private static final String SQL_COUNT_TABLES =
      """
      SELECT count(*) FROM information_schema.tables
      WHERE table_schema = 'public'
        AND table_name IN ('tasks', 'signups', 'inventory', 'outbox')\
      """;

  private static final int FIRST_COLUMN = 1;
  private static final long EVERY_TABLE = 4L;

  @Test
  @DisplayName("creates every table the stores read")
  void createsEveryTable() {
    assertThat(count(SQL_COUNT_TABLES)).isEqualTo(EVERY_TABLE);
  }

  @Test
  @DisplayName("applies twice without changing anything, so starting twice is not an error")
  void appliesTwiceWithoutChangingAnything() {
    long seeded = count(SQL_COUNT_INVENTORY);

    assertThatCode(() -> new Schema(database()).apply()).doesNotThrowAnyException();

    assertThat(count(SQL_COUNT_INVENTORY))
        .as("a second pass must not duplicate the seed rows")
        .isEqualTo(seeded);
    assertThat(seeded).isEqualTo(InventorySeed.items().size());
  }

  private static long count(String sql) {
    return database()
        .query(
            connection -> {
              try (PreparedStatement query = connection.prepareStatement(sql);
                  ResultSet rows = query.executeQuery()) {
                rows.next();
                return rows.getLong(FIRST_COLUMN);
              }
            });
  }
}
