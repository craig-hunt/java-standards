package com.sagecrest.standards.infrastructure.stores;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.domain.signups.EmailAddress;
import com.sagecrest.standards.domain.signups.FullName;
import com.sagecrest.standards.domain.signups.Notes;
import com.sagecrest.standards.domain.signups.Plan;
import com.sagecrest.standards.domain.signups.Seats;
import com.sagecrest.standards.domain.signups.Signup;
import com.sagecrest.standards.domain.signups.SignupConstants;
import com.sagecrest.standards.domain.signups.SignupId;
import com.sagecrest.standards.infrastructure.persistence.PostgresFixture;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JdbcSignupStoreIT extends PostgresFixture {

  private static final String NAME = "Ada Lovelace";
  private static final String EMAIL = "ada@example.com";
  private static final String NOTES = "Platform team first.";
  private static final int SEATS = 3;

  private static final Instant RECORDED_AT = Instant.parse("2026-10-03T13:45:12Z");
  private static final Clock FROZEN = Clock.fixed(RECORDED_AT, ZoneOffset.UTC);

  private static final String SQL_READ_OUTBOX =
      "SELECT type, payload, occurred_at, published_at FROM outbox";
  private static final String SQL_COUNT_SIGNUPS = "SELECT count(*) FROM signups";
  private static final String COLUMN_TYPE = "type";
  private static final String COLUMN_PAYLOAD = "payload";
  private static final String COLUMN_PUBLISHED_AT = "published_at";
  private static final String COLUMN_OCCURRED_AT = "occurred_at";
  private static final String EXPECTED_WIRE_NAME = "SignupRecorded";
  private static final int FIRST_COLUMN = 1;
  private static final long ONE_ROW = 1L;

  private final JdbcSignupStore store = new JdbcSignupStore(database(), FROZEN);

  private static Signup signup() {
    return new Signup(
        new FullName(NAME),
        new EmailAddress(EMAIL),
        new Plan(SignupConstants.PLAN_GROWTH),
        new Seats(SEATS),
        new Notes(NOTES));
  }

  @Test
  @DisplayName("hands back the identifier the database assigned")
  void handsBackTheAssignedIdentifier() {
    SignupId id = store.save(signup());

    assertThat(id.value()).isPositive();
    assertThat(countSignups()).isEqualTo(ONE_ROW);
  }

  @Test
  @DisplayName("writes the signup row and the outbox row in the same transaction")
  void writesBothRowsTogether() {
    SignupId id = store.save(signup());

    database()
        .query(
            connection -> {
              try (PreparedStatement read = connection.prepareStatement(SQL_READ_OUTBOX);
                  ResultSet rows = read.executeQuery()) {
                assertThat(rows.next()).as("an outbox row accompanies the signup").isTrue();
                assertThat(rows.getString(COLUMN_TYPE)).isEqualTo(EXPECTED_WIRE_NAME);
                assertThat(rows.getString(COLUMN_PAYLOAD))
                    .contains(EMAIL, SignupConstants.PLAN_GROWTH, String.valueOf(id.value()));
                assertThat(rows.getObject(COLUMN_PUBLISHED_AT))
                    .as("a freshly written message has not been delivered yet")
                    .isNull();
                assertThat(rows.next()).as("exactly one outbox row").isFalse();
              }
              return null;
            });
  }

  @Test
  @DisplayName("stamps the event with the injected clock rather than the wall clock")
  void stampsTheEventWithTheInjectedClock() {
    store.save(signup());

    assertThat(occurredAt()).isEqualTo(RECORDED_AT);
  }

  private static Instant occurredAt() {
    return database()
        .query(
            connection -> {
              try (PreparedStatement read = connection.prepareStatement(SQL_READ_OUTBOX);
                  ResultSet rows = read.executeQuery()) {
                rows.next();
                return rows.getObject(COLUMN_OCCURRED_AT, OffsetDateTime.class).toInstant();
              }
            });
  }

  private static long countSignups() {
    return database()
        .query(
            connection -> {
              try (PreparedStatement count = connection.prepareStatement(SQL_COUNT_SIGNUPS);
                  ResultSet rows = count.executeQuery()) {
                rows.next();
                return rows.getLong(FIRST_COLUMN);
              }
            });
  }
}
