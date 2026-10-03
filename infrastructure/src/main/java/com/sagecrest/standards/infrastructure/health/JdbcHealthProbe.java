package com.sagecrest.standards.infrastructure.health;

import com.sagecrest.standards.application.ports.HealthProbe;
import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import com.sagecrest.standards.infrastructure.persistence.Database;
import java.sql.PreparedStatement;

/**
 * Asks the database to answer something trivial.
 *
 * <p>It runs a statement rather than merely opening a connection, because a pooled connection can
 * be handed over after the server on the other end has gone away. Only a round trip proves the
 * database is still there.
 */
public final class JdbcHealthProbe implements HealthProbe {

  private final Database database;

  public JdbcHealthProbe(Database database) {
    this.database = database;
  }

  @Override
  public void ping() {
    database.query(
        connection -> {
          try (PreparedStatement query =
              connection.prepareStatement(InfrastructureConstants.SQL_PING)) {
            return query.executeQuery().next();
          }
        });
  }
}
