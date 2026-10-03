package com.sagecrest.standards.domain.tasks;

import com.sagecrest.standards.domain.text.TextLength;
import java.util.Objects;

/**
 * A validated task title: trimmed, present, and within the length limit.
 *
 * <p>The compact constructor trims the value it stores, so the trimming and the checks cannot
 * disagree and no caller can hold an untrimmed title. It counts code points rather than UTF-16 code
 * units, so the limit lands in the same place as the Go and C# siblings.
 *
 * <p>It accepts null because absence is what arrives from the edge when a JSON body omits the
 * field, and this type exists to be the single place that decides what absence means. A signature
 * that rejected null would push that decision back out to every caller.
 */
public record TaskTitle(String value) {

  private static final String ABSENT = "";

  public TaskTitle {
    value = Objects.requireNonNullElse(value, ABSENT).trim();
    if (value.isEmpty()) {
      throw TaskErrors.titleRequired();
    }
    if (TextLength.countCodePoints(value) > TaskConstants.MAX_TITLE_LENGTH) {
      throw TaskErrors.titleTooLong();
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
