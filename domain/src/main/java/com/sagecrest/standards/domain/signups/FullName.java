package com.sagecrest.standards.domain.signups;

import java.util.Objects;

/**
 * A signup's name: trimmed and present.
 *
 * <p>The compact constructor is the only way in, so no {@link Signup} can hold a blank name however
 * it was built. A plain {@code String} field would have put that guarantee in whichever caller
 * happened to remember the validator.
 */
public record FullName(String value) {

  private static final String ABSENT = "";

  public FullName {
    value = Objects.requireNonNullElse(value, ABSENT).trim();
    if (value.isEmpty()) {
      throw SignupErrors.nameRequired();
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
