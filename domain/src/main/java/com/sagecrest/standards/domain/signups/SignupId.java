package com.sagecrest.standards.domain.signups;

/** A signup identifier, distinct from every other identifier in the system. */
public record SignupId(long value) {

  private static final long FIRST_VALID_ID = 1;

  public SignupId {
    if (value < FIRST_VALID_ID) {
      throw SignupErrors.invalidId();
    }
  }

  @Override
  public String toString() {
    return Long.toString(value);
  }
}
