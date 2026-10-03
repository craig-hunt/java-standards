package com.sagecrest.standards.domain.tasks;

import java.util.Optional;

/**
 * A task identifier, distinct from every other identifier in the system.
 *
 * <p>A {@code long} would compile just as well in every position an identifier appears, which is
 * exactly the problem: passing a signup id where a task id belongs would type-check. A record costs
 * one wrapper and makes that mistake impossible.
 *
 * <p>The validation sits in the compact constructor rather than in a static factory, so no path
 * reaches an invalid instance. The C# sibling needs a second unvalidated factory because EF
 * round-trips a key through its converter while deciding whether the key has been set, and the read
 * direction therefore has to accept a zero the domain would reject. Nothing here maps through a
 * converter, so this type keeps one way in.
 */
public record TaskId(long value) {

  private static final long FIRST_VALID_ID = 1;

  private static final char FIRST_DIGIT = '0';
  private static final char LAST_DIGIT = '9';

  public TaskId {
    if (value < FIRST_VALID_ID) {
      throw TaskErrors.invalidId();
    }
  }

  /**
   * Reads an identifier from a route value, answering empty when the text does not name one.
   *
   * <p>The scan accepts ASCII digits and nothing else. {@code Long.parseLong} would accept a
   * leading sign, and {@code Character.isDigit} would accept Arabic-Indic and other decimal digits
   * that {@code parseLong} then converts, so either on its own would let a request address a task
   * by a spelling the Go and C# siblings reject. All three agree on which requests reach a handler
   * at all.
   */
  public static Optional<TaskId> parse(String raw) {
    if (raw == null || raw.isEmpty()) {
      return Optional.empty();
    }
    for (int index = 0; index < raw.length(); index++) {
      char character = raw.charAt(index);
      if (character < FIRST_DIGIT || character > LAST_DIGIT) {
        return Optional.empty();
      }
    }
    try {
      long parsed = Long.parseLong(raw);
      return parsed < FIRST_VALID_ID ? Optional.empty() : Optional.of(new TaskId(parsed));
    } catch (NumberFormatException tooLargeForALong) {
      return Optional.empty();
    }
  }

  @Override
  public String toString() {
    return Long.toString(value);
  }
}
