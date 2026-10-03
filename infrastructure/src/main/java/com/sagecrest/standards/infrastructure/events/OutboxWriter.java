package com.sagecrest.standards.infrastructure.events;

import com.sagecrest.standards.domain.events.DomainEvent;
import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import com.sagecrest.standards.infrastructure.persistence.Columns;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZoneOffset;

/**
 * Writes one outbox row on a connection a caller already owns.
 *
 * <p>It takes the connection rather than a {@link
 * com.sagecrest.standards.infrastructure.persistence.Database} on purpose. The whole value of the
 * outbox is that the row commits with the state change that produced it, so a writer that opened
 * its own transaction would quietly undo the guarantee while appearing to provide it.
 *
 * <p>Shared by every store that announces something. Two copies of this insert would eventually
 * disagree about a column, and the disagreement would surface as a message nobody could
 * deserialize.
 */
public final class OutboxWriter {

  public static void write(Connection connection, DomainEvent event) throws SQLException {
    OutboxMessage row = OutboxSerializer.toRow(event);
    try (PreparedStatement insert =
        connection.prepareStatement(InfrastructureConstants.SQL_INSERT_OUTBOX)) {
      insert.setObject(Columns.FIRST, row.eventId());
      insert.setString(Columns.SECOND, row.type());
      insert.setString(Columns.THIRD, row.payload());
      insert.setObject(Columns.FOURTH, row.occurredAt().atOffset(ZoneOffset.UTC));
      insert.executeUpdate();
    }
  }

  private OutboxWriter() {}
}
