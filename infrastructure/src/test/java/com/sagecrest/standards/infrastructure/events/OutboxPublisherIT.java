package com.sagecrest.standards.infrastructure.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.application.events.FanOutEventDispatcher;
import com.sagecrest.standards.application.ports.EventConsumer;
import com.sagecrest.standards.domain.events.DomainEvent;
import com.sagecrest.standards.domain.signups.Plan;
import com.sagecrest.standards.domain.signups.Signup;
import com.sagecrest.standards.domain.signups.SignupConstants;
import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import com.sagecrest.standards.infrastructure.persistence.Columns;
import com.sagecrest.standards.infrastructure.persistence.PostgresFixture;
import com.sagecrest.standards.infrastructure.stores.JdbcSignupStore;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutboxPublisherIT extends PostgresFixture {

  private static final String NAME = "Ada Lovelace";
  private static final String EMAIL = "ada@example.com";
  private static final String NOTES = "Platform team first.";
  private static final int SEATS = 3;

  private static final Instant RECORDED_AT = Instant.parse("2026-10-03T13:45:12Z");
  private static final Instant PUBLISHED_AT = Instant.parse("2026-10-03T13:45:30Z");
  private static final Clock AT_RECORDING = Clock.fixed(RECORDED_AT, ZoneOffset.UTC);
  private static final Clock AT_PUBLISHING = Clock.fixed(PUBLISHED_AT, ZoneOffset.UTC);

  private static final String UNRESOLVABLE_TYPE = "SomethingFromALaterRelease";
  private static final String EMPTY_PAYLOAD = "{}";

  private static final String SQL_COUNT_PENDING =
      "SELECT count(*) FROM outbox WHERE published_at IS NULL";
  private static final String SQL_READ_PUBLISHED_AT =
      "SELECT published_at FROM outbox WHERE event_id = ?";
  private static final String COLUMN_PUBLISHED_AT = "published_at";

  private static final int FIRST_COLUMN = 1;
  private static final int NONE = 0;
  private static final int ONE = 1;
  private static final long NO_ROWS = 0L;
  private static final long ONE_ROW = 1L;

  private final List<DomainEvent> delivered = new ArrayList<>();

  private final EventConsumer everything =
      new EventConsumer() {
        @Override
        public boolean accepts(DomainEvent event) {
          return true;
        }

        @Override
        public void consume(DomainEvent event) {
          delivered.add(event);
        }
      };

  private OutboxPublisher publisher() {
    return new OutboxPublisher(
        database(), new FanOutEventDispatcher(List.of(everything)), AT_PUBLISHING);
  }

  private void recordASignup() {
    new JdbcSignupStore(database(), AT_RECORDING)
        .save(new Signup(NAME, EMAIL, new Plan(SignupConstants.PLAN_GROWTH), SEATS, NOTES));
  }

  @Test
  @DisplayName("delivers a pending message and marks it, so a second pass finds nothing")
  void deliversOnceAndMarksIt() {
    recordASignup();

    assertThat(publisher().publishPending()).isEqualTo(ONE);
    assertThat(delivered).hasSize(ONE);
    assertThat(countPending()).isEqualTo(NO_ROWS);

    assertThat(publisher().publishPending()).isEqualTo(NONE);
    assertThat(delivered).hasSize(ONE);
  }

  @Test
  @DisplayName("stamps the delivery with the publisher's clock, not the recording clock")
  void stampsTheDeliveryWithThePublisherClock() {
    recordASignup();
    publisher().publishPending();

    assertThat(publishedAt()).isEqualTo(PUBLISHED_AT);
  }

  @Test
  void publishesNothingFromAnEmptyOutbox() {
    assertThat(publisher().publishPending()).isEqualTo(NONE);
    assertThat(delivered).isEmpty();
  }

  @Test
  @DisplayName("leaves a message it cannot resolve pending rather than acknowledging it")
  void leavesAnUnresolvableMessagePending() {
    insertUnresolvable();

    assertThat(publisher().publishPending()).isEqualTo(NONE);
    assertThat(delivered).isEmpty();
    assertThat(countPending())
        .as("the message waits for a deployment that knows its type")
        .isEqualTo(ONE_ROW);
  }

  @Test
  @DisplayName("an unresolvable message does not block the resolvable one beside it")
  void anUnresolvableMessageDoesNotBlockTheOthers() {
    insertUnresolvable();
    recordASignup();

    assertThat(publisher().publishPending()).isEqualTo(ONE);
    assertThat(delivered).hasSize(ONE);
    assertThat(countPending()).isEqualTo(ONE_ROW);
  }

  private static void insertUnresolvable() {
    database()
        .inTransaction(
            connection -> {
              try (PreparedStatement insert =
                  connection.prepareStatement(InfrastructureConstants.SQL_INSERT_OUTBOX)) {
                insert.setObject(Columns.FIRST, UUID.randomUUID());
                insert.setString(Columns.SECOND, UNRESOLVABLE_TYPE);
                insert.setString(Columns.THIRD, EMPTY_PAYLOAD);
                // Earlier than the signup, so the claim returns it first and the test
                // proves the publisher steps over it rather than stopping at it.
                insert.setObject(
                    Columns.FOURTH, RECORDED_AT.minusSeconds(ONE).atOffset(ZoneOffset.UTC));
                insert.executeUpdate();
              }
              return null;
            });
  }

  private static long countPending() {
    return database()
        .query(
            connection -> {
              try (PreparedStatement count = connection.prepareStatement(SQL_COUNT_PENDING);
                  ResultSet rows = count.executeQuery()) {
                rows.next();
                return rows.getLong(FIRST_COLUMN);
              }
            });
  }

  private Instant publishedAt() {
    UUID eventId = delivered.getFirst().eventId();
    return database()
        .query(
            connection -> {
              try (PreparedStatement read = connection.prepareStatement(SQL_READ_PUBLISHED_AT)) {
                read.setObject(Columns.FIRST, eventId);
                try (ResultSet rows = read.executeQuery()) {
                  rows.next();
                  return rows.getObject(COLUMN_PUBLISHED_AT, OffsetDateTime.class).toInstant();
                }
              }
            });
  }
}
