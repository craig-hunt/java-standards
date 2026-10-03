package com.sagecrest.standards.domain.signups;

import com.sagecrest.standards.domain.text.TextLength;
import java.util.Objects;

/**
 * Whatever a signup wanted to add, within the length limit.
 *
 * <p>Empty is valid: notes are optional, and absence is not a problem to report. The limit counts
 * code points, so the boundary lands in the same place as the Go and C# siblings.
 */
public record Notes(String value) {

  private static final String ABSENT = "";

  public Notes {
    value = Objects.requireNonNullElse(value, ABSENT).trim();
    if (TextLength.countCodePoints(value) > SignupConstants.MAX_NOTES_LENGTH) {
      throw SignupErrors.notesTooLong();
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
