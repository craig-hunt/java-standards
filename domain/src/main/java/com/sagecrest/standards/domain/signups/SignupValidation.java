package com.sagecrest.standards.domain.signups;

import java.util.Map;
import java.util.Optional;

/**
 * The outcome of validating a signup: either the validated signup, or the problems keyed by the
 * field each one belongs to.
 *
 * <p>The signup arrives as an {@link Optional} rather than a nullable field, so a caller that reads
 * the value without checking the problems does not compile. The C# sibling exposes a nullable
 * record and a {@code Valid} flag, which a caller can read in the wrong order.
 */
public record SignupValidation(Optional<Signup> value, Map<String, String> problems) {

  public SignupValidation {
    problems = Map.copyOf(problems);
  }

  public boolean valid() {
    return problems.isEmpty();
  }
}
