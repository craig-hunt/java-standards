package com.sagecrest.standards.domain.signups;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A signup's email: trimmed, present, and shaped like an address.
 *
 * <p>It tells an absent address apart from a malformed one, because the form shows a different
 * sentence for each and the difference is information the caller already has.
 *
 * <p>The pattern is deliberately coarse. A regular expression that accepted exactly RFC 5322 would
 * reject addresses that work and accept ones that do not; the only test that settles it is sending
 * a message. This rejects what is obviously wrong and leaves the rest to delivery.
 */
public record EmailAddress(String value) {

  private static final String ABSENT = "";
  private static final Pattern SHAPE = Pattern.compile(SignupConstants.EMAIL_PATTERN);

  public EmailAddress {
    value = Objects.requireNonNullElse(value, ABSENT).trim();
    if (value.isEmpty()) {
      throw SignupErrors.emailRequired();
    }
    if (!SHAPE.matcher(value).matches()) {
      throw SignupErrors.emailInvalid();
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
