package com.sagecrest.standards.infrastructure.stores;

import com.sagecrest.standards.application.ports.SignupStore;
import com.sagecrest.standards.domain.events.SignupRecorded;
import com.sagecrest.standards.domain.signups.Signup;
import com.sagecrest.standards.domain.signups.SignupId;
import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import com.sagecrest.standards.infrastructure.events.OutboxWriter;
import com.sagecrest.standards.infrastructure.persistence.Columns;
import com.sagecrest.standards.infrastructure.persistence.Database;
import com.sagecrest.standards.infrastructure.persistence.InfrastructureException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Records signups, and the event that says one happened.
 *
 * <p>The signup row and the outbox row commit together. The database assigns the identifier on the
 * first insert and the event carries it, so the two statements run inside one transaction: either
 * both rows land or neither does. No consumer ever hears about a signup that failed to store, and
 * no stored signup goes unannounced.
 *
 * <p>The clock arrives as {@link Clock}, which the JDK already defines. The C# sibling declares an
 * {@code IClock} port because .NET has no equivalent seam; here inventing one would add a port
 * whose only implementation delegates to the platform, and would cost a test the ready-made {@code
 * Clock.fixed}.
 */
public final class JdbcSignupStore implements SignupStore {

  private final Database database;
  private final Clock clock;

  public JdbcSignupStore(Database database, Clock clock) {
    this.database = database;
    this.clock = clock;
  }

  @Override
  public SignupId save(Signup signup) {
    return database.inTransaction(
        connection -> {
          Instant now = clock.instant();
          SignupId id = insertSignup(connection, signup, now);
          SignupRecorded recorded = signup.recorded(id, UUID.randomUUID(), now);
          OutboxWriter.write(connection, recorded);
          return id;
        });
  }

  private static SignupId insertSignup(Connection connection, Signup signup, Instant now)
      throws SQLException {
    try (PreparedStatement insert =
        connection.prepareStatement(InfrastructureConstants.SQL_INSERT_SIGNUP)) {
      insert.setString(Columns.FIRST, signup.fullName().value());
      insert.setString(Columns.SECOND, signup.email().value());
      insert.setString(Columns.THIRD, signup.plan().value());
      insert.setInt(Columns.FOURTH, signup.seats().value());
      insert.setString(Columns.FIFTH, signup.notes().value());
      insert.setObject(Columns.SIXTH, utc(now));
      try (ResultSet rows = insert.executeQuery()) {
        if (!rows.next()) {
          throw new InfrastructureException(InfrastructureConstants.MSG_ROW_VANISHED);
        }
        return new SignupId(rows.getLong(InfrastructureConstants.COLUMN_ID));
      }
    }
  }

  private static OffsetDateTime utc(Instant moment) {
    return moment.atOffset(ZoneOffset.UTC);
  }
}
