package com.sagecrest.standards.application.health;

import java.util.Optional;

/**
 * The outcome of a readiness probe: whether the dependency answered, and the failure to log when it
 * did not.
 *
 * <p>The failure arrives as an {@link Optional} rather than a nullable field, so reading it without
 * checking does not compile.
 *
 * <p>The factories read {@code available} and {@code unavailable} rather than {@code ready} and
 * {@code unavailable}, because a record's accessor already owns the name {@code ready()} and a
 * static method cannot share it.
 */
public record ReadinessCheck(boolean ready, Optional<Throwable> failure) {

  public static ReadinessCheck available() {
    return new ReadinessCheck(true, Optional.empty());
  }

  public static ReadinessCheck unavailable(Throwable failure) {
    return new ReadinessCheck(false, Optional.of(failure));
  }
}
