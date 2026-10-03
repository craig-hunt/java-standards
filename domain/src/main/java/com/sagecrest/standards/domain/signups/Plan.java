package com.sagecrest.standards.domain.signups;

import java.util.Objects;
import java.util.Set;

/**
 * The plan a signup names, kept as the value the form submitted.
 *
 * <p>An enum would collapse an absent plan and an unrecognized one into the same undefined member,
 * and the form shows a different sentence for each. Holding the submitted text keeps that
 * distinction available to the validator.
 */
public record Plan(String value) {

  private static final String ABSENT = "";

  private static final Set<String> KNOWN =
      Set.of(
          SignupConstants.PLAN_STARTER,
          SignupConstants.PLAN_GROWTH,
          SignupConstants.PLAN_ENTERPRISE);

  public Plan {
    value = Objects.requireNonNullElse(value, ABSENT);
  }

  public boolean isAbsent() {
    return value.isEmpty();
  }

  public boolean known() {
    return KNOWN.contains(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
