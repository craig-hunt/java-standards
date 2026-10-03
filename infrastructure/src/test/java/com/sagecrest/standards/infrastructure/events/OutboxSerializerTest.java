package com.sagecrest.standards.infrastructure.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.domain.events.DomainEvent;
import com.sagecrest.standards.domain.events.SignupRecorded;
import com.sagecrest.standards.domain.events.TaskCompleted;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutboxSerializerTest {

  private static final String EMAIL = "ada@example.com";
  private static final String PLAN = "Growth";
  private static final String TITLE = "Read the ADR";
  private static final int SEATS = 3;
  private static final long ID = 7L;

  private static final String SIGNUP_WIRE_NAME = "SignupRecorded";
  private static final String TASK_WIRE_NAME = "TaskCompleted";
  private static final String UNKNOWN_TYPE = "SomethingFromALaterRelease";
  private static final String NOT_JSON = "{";
  private static final String EMPTY_PAYLOAD = "{}";

  /** Truncated to milliseconds, which is the precision a timestamptz column keeps. */
  private static final Instant OCCURRED = Instant.parse("2026-10-03T13:45:12.345Z");

  private static final UUID EVENT_ID = UUID.fromString("0d6f2f1e-4c3a-4b7a-9f2e-8c1d5a6b7c8d");

  private static SignupRecorded signupRecorded() {
    return new SignupRecorded(EVENT_ID, OCCURRED, ID, EMAIL, PLAN, SEATS);
  }

  @Test
  @DisplayName("writes a stable wire name rather than one derived from the class")
  void writesAStableWireName() {
    assertThat(OutboxSerializer.toRow(signupRecorded()).type()).isEqualTo(SIGNUP_WIRE_NAME);
    assertThat(OutboxSerializer.toRow(new TaskCompleted(EVENT_ID, OCCURRED, ID, TITLE)).type())
        .isEqualTo(TASK_WIRE_NAME);
  }

  @Test
  void carriesTheIdentifierAndMomentOutsideThePayload() {
    OutboxMessage row = OutboxSerializer.toRow(signupRecorded());

    assertThat(row.eventId()).isEqualTo(EVENT_ID);
    assertThat(row.occurredAt()).isEqualTo(OCCURRED);
  }

  @Test
  @DisplayName("writes the moment as text, so no consumer reads a rounded float")
  void writesTheMomentAsText() {
    assertThat(OutboxSerializer.toRow(signupRecorded()).payload()).contains(OCCURRED.toString());
  }

  @Test
  void readsBackEverySignupFieldItWrote() {
    Optional<DomainEvent> read = OutboxSerializer.fromRow(OutboxSerializer.toRow(signupRecorded()));

    assertThat(read).contains(signupRecorded());
  }

  @Test
  void readsBackEveryTaskFieldItWrote() {
    TaskCompleted completed = new TaskCompleted(EVENT_ID, OCCURRED, ID, TITLE);

    assertThat(OutboxSerializer.fromRow(OutboxSerializer.toRow(completed))).contains(completed);
  }

  @Test
  @DisplayName("refuses a type this deployment cannot resolve rather than guessing one")
  void refusesAnUnresolvableType() {
    OutboxMessage row = new OutboxMessage(EVENT_ID, UNKNOWN_TYPE, EMPTY_PAYLOAD, OCCURRED);

    assertThat(OutboxSerializer.fromRow(row)).isEmpty();
  }

  @Test
  void refusesAPayloadItCannotParse() {
    OutboxMessage row = new OutboxMessage(EVENT_ID, SIGNUP_WIRE_NAME, NOT_JSON, OCCURRED);

    assertThat(OutboxSerializer.fromRow(row)).isEmpty();
  }
}
