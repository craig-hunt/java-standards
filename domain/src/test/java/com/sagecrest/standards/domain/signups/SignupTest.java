package com.sagecrest.standards.domain.signups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.domain.errors.ValidationException;
import com.sagecrest.standards.domain.events.SignupRecorded;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignupTest {

  private static final String NAME = "Ada Lovelace";
  private static final String EMAIL = "ada@example.com";
  private static final String NOTES = "Platform team first.";
  private static final int SEATS = 3;
  private static final long ID = 7L;
  private static final long BELOW_FIRST_VALID = 0L;
  private static final String EXPECTED_SUMMARY = "Ada Lovelace on the Growth plan, 3 seat(s).";

  private static Signup signup() {
    return new Signup(NAME, EMAIL, new Plan(SignupConstants.PLAN_GROWTH), SEATS, NOTES);
  }

  @Test
  void describesItselfInOneSentence() {
    assertThat(signup().summary()).isEqualTo(EXPECTED_SUMMARY);
  }

  @Test
  @DisplayName("composes its own event, carrying primitives rather than domain types")
  void composesItsOwnEvent() {
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();

    SignupRecorded recorded = signup().recorded(new SignupId(ID), eventId, occurredAt);

    assertThat(recorded.eventId()).isEqualTo(eventId);
    assertThat(recorded.occurredAt()).isEqualTo(occurredAt);
    assertThat(recorded.signupId()).isEqualTo(ID);
    assertThat(recorded.email()).isEqualTo(EMAIL);
    assertThat(recorded.plan()).isEqualTo(SignupConstants.PLAN_GROWTH);
    assertThat(recorded.seats()).isEqualTo(SEATS);
  }

  @Test
  void rejectsASignupIdBelowTheFirstValidValue() {
    assertThatThrownBy(() -> new SignupId(BELOW_FIRST_VALID))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).code())
        .isEqualTo(SignupConstants.CODE_INVALID_ID);
  }

  @Test
  void printsTheBareSignupId() {
    assertThat(new SignupId(ID)).hasToString(Long.toString(ID));
  }
}
