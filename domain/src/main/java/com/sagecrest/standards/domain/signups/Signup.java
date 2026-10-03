package com.sagecrest.standards.domain.signups;

import com.sagecrest.standards.domain.events.SignupRecorded;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/** A signup that passed validation, so every field already holds a usable value. */
public record Signup(String fullName, String email, Plan plan, int seats, String notes) {

  public String summary() {
    return String.format(
        Locale.ROOT, SignupConstants.SUMMARY_FORMAT, fullName, plan.value(), seats);
  }

  /**
   * Describes this signup as the event a consumer receives.
   *
   * <p>The signup composes its own event, so the fact travels with the rules that produced it
   * rather than being assembled by whichever layer happens to write the row. The identifier arrives
   * from the store because the database assigns it, and the clock arrives from the caller because
   * the domain owns no clock.
   */
  public SignupRecorded recorded(SignupId id, UUID eventId, Instant occurredAt) {
    return new SignupRecorded(eventId, occurredAt, id.value(), email, plan.value(), seats);
  }
}
