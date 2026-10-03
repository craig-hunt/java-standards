package com.sagecrest.standards.domain.signups;

import java.util.Objects;
import java.util.Set;

/**
 * The plan a signup names.
 *
 * <p>It holds the submitted text rather than an enum constant, because an absent plan and an
 * unrecognized one need different sentences and an enum collapses both into the same undefined
 * member. The constructor keeps that distinction by throwing a different failure for each, so the
 * rule lives here once instead of in the validator and the type both.
 */
public record Plan(String value) {

  private static final String ABSENT = "";

  private static final Set<String> KNOWN =
      Set.of(
          SignupConstants.PLAN_STARTER,
          SignupConstants.PLAN_GROWTH,
          SignupConstants.PLAN_ENTERPRISE);

  public Plan {
    value = Objects.requireNonNullElse(value, ABSENT).trim();
    if (value.isEmpty()) {
      throw SignupErrors.planRequired();
    }
    if (!KNOWN.contains(value)) {
      throw SignupErrors.planUnknown();
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
