package com.sagecrest.standards.infrastructure.stores;

import com.sagecrest.standards.application.ports.InventoryStore;
import com.sagecrest.standards.domain.inventory.InventoryItem;
import com.sagecrest.standards.domain.inventory.Status;
import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import com.sagecrest.standards.infrastructure.persistence.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Stock rows, in PostgreSQL.
 *
 * <p>It reads every row and orders by name, and nothing else. Searching and ordering by the
 * requested column stay in {@code InventoryQuery}, so a second store cannot answer the same request
 * in a different order. The {@code ORDER BY} here exists to make the input to that sort stable, so
 * rows that tie keep a predictable order between requests.
 */
public final class JdbcInventoryStore implements InventoryStore {

  private final Database database;

  public JdbcInventoryStore(Database database) {
    this.database = database;
  }

  @Override
  public List<InventoryItem> items() {
    return database.query(
        connection -> {
          try (PreparedStatement query =
                  connection.prepareStatement(InfrastructureConstants.SQL_LIST_INVENTORY);
              ResultSet rows = query.executeQuery()) {
            List<InventoryItem> items = new ArrayList<>();
            while (rows.next()) {
              items.add(
                  new InventoryItem(
                      rows.getString(InfrastructureConstants.COLUMN_NAME),
                      rows.getInt(InfrastructureConstants.COLUMN_QUANTITY),
                      new Status(rows.getString(InfrastructureConstants.COLUMN_STATUS))));
            }
            return List.copyOf(items);
          }
        });
  }
}
