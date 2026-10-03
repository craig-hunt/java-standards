package com.sagecrest.standards.infrastructure.persistence;

import com.sagecrest.standards.domain.inventory.InventoryItem;
import com.sagecrest.standards.domain.inventory.InventorySeed;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * Applies the schema and the seed rows.
 *
 * <p>Both steps repeat harmlessly, so starting the service twice against the same database is not
 * an error. A reference implementation that demanded a clean database would teach a workaround
 * before it taught anything else.
 *
 * <p>The seed rows come from {@link InventorySeed} rather than from the SQL script. Writing the six
 * names in both places would leave nothing to keep them equal, and the first time one drifted the
 * tests would still pass against whichever copy they happened to read.
 */
public final class Schema {

  private static final String SCRIPT = "/schema.sql";

  private static final String SQL_SEED_INVENTORY =
      """
      INSERT INTO inventory (name, quantity, status)
      VALUES (?, ?, ?)
      ON CONFLICT (name) DO NOTHING\
      """;

  private static final String MSG_SCRIPT_MISSING = "the schema script is not on the classpath";
  private static final String MSG_SCRIPT_UNREADABLE = "the schema script could not be read";

  private final Database database;

  public Schema(Database database) {
    this.database = database;
  }

  public void apply() {
    String script = script();
    database.inTransaction(
        connection -> {
          try (Statement statement = connection.createStatement()) {
            statement.execute(script);
          }
          try (PreparedStatement seed = connection.prepareStatement(SQL_SEED_INVENTORY)) {
            for (InventoryItem item : InventorySeed.items()) {
              seed.setString(Columns.SEED_NAME, item.name());
              seed.setInt(Columns.SEED_QUANTITY, item.quantity());
              seed.setString(Columns.SEED_STATUS, item.status().value());
              seed.addBatch();
            }
            seed.executeBatch();
          }
          return null;
        });
  }

  private static String script() {
    try (InputStream source = Schema.class.getResourceAsStream(SCRIPT)) {
      if (source == null) {
        throw new InfrastructureException(MSG_SCRIPT_MISSING);
      }
      return new String(source.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException unreadable) {
      throw new InfrastructureException(MSG_SCRIPT_UNREADABLE, unreadable);
    }
  }
}
