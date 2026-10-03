package com.sagecrest.standards.domain.signups;

import com.sagecrest.standards.domain.events.SignupRecorded;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * A signup whose every field already holds a usable value.
 *
 * <p>The guarantee is in the types, not in a convention. Each component validates in its own
 * constructor, so there is no way to build this record out of a blank name or an unknown plan, and
 * no caller has to remember to run {@link SignupValidator} first. An earlier version held plain
 * strings and an int, and relying on every caller to validate ahead of construction is exactly the
 * arrangement this repository argues against.
 *
 * <p>{@link SignupValidator} still exists, and still earns its place: it reports every problem at
 * once, which a constructor cannot do because it can only throw the first one it finds.
 */
public record Signup(FullName fullName, EmailAddress email, Plan plan, Seats seats, Notes notes) {

  public String summary() {
    return String.format(
        Locale.ROOT, SignupConstants.SUMMARY_FORMAT, fullName.value(), plan.value(), seats.value());
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
    return new SignupRecorded(
        eventId, occurredAt, id.value(), email.value(), plan.value(), seats.value());
  }
}
