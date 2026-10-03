package com.sagecrest.standards.infrastructure.events;

import com.sagecrest.standards.application.ports.EventDispatcher;
import com.sagecrest.standards.domain.events.DomainEvent;
import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import com.sagecrest.standards.infrastructure.persistence.Columns;
import com.sagecrest.standards.infrastructure.persistence.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Claims a batch of outbox messages, delivers them, and marks what it delivered.
 *
 * <p>A message whose type this deployment cannot resolve stays unpublished. Marking it would
 * acknowledge something no consumer ever saw, which is the one outcome the outbox exists to
 * prevent. It waits for a deployment that knows the type, and a production system would move it
 * aside once a retry budget ran out.
 *
 * <p>Separate from the relay, so the delivery path has a seam a test can drive without a background
 * thread and a timer.
 */
public final class OutboxPublisher {

  private static final Logger LOG = LoggerFactory.getLogger(OutboxPublisher.class);

  private static final int NONE = 0;

  private final Database database;
  private final EventDispatcher dispatcher;
  private final Clock clock;

  public OutboxPublisher(Database database, EventDispatcher dispatcher, Clock clock) {
    this.database = database;
    this.dispatcher = dispatcher;
    this.clock = clock;
  }

  /** Delivers one batch and answers how many it published. */
  public int publishPending() {
    return database.inTransaction(
        connection -> {
          List<OutboxMessage> claimed = claim(connection);
          OffsetDateTime publishedAt =
              OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
          int published = NONE;

          try (PreparedStatement mark =
              connection.prepareStatement(InfrastructureConstants.SQL_MARK_PUBLISHED)) {
            for (OutboxMessage row : claimed) {
              Optional<DomainEvent> event = OutboxSerializer.fromRow(row);
              if (event.isEmpty()) {
                LOG.error(
                    InfrastructureConstants.MSG_OUTBOX_UNKNOWN_TYPE, row.eventId(), row.type());
                continue;
              }
              dispatcher.dispatch(event.get());
              mark.setObject(Columns.FIRST, publishedAt);
              mark.setObject(Columns.SECOND, row.eventId());
              mark.addBatch();
              published++;
            }
            // No guard on an empty batch. executeBatch on one sends nothing to the
            // server and returns an empty array, so a check for it would be a branch
            // no test could tell the difference about.
            mark.executeBatch();
          }
          return published;
        });
  }

  private static List<OutboxMessage> claim(Connection connection) throws SQLException {
    try (PreparedStatement query =
        connection.prepareStatement(InfrastructureConstants.SQL_CLAIM_OUTBOX)) {
      query.setInt(Columns.FIRST, InfrastructureConstants.OUTBOX_BATCH_SIZE);
      try (ResultSet rows = query.executeQuery()) {
        List<OutboxMessage> claimed = new ArrayList<>();
        while (rows.next()) {
          claimed.add(
              new OutboxMessage(
                  rows.getObject(InfrastructureConstants.COLUMN_EVENT_ID, UUID.class),
                  rows.getString(InfrastructureConstants.COLUMN_TYPE),
                  rows.getString(InfrastructureConstants.COLUMN_PAYLOAD),
                  rows.getObject(InfrastructureConstants.COLUMN_OCCURRED_AT, OffsetDateTime.class)
                      .toInstant()));
        }
        return claimed;
      }
    }
  }
}
