package com.sagecrest.standards.domain.health;

import java.time.Duration;

/** Every literal the health feature carries, named once. */
public final class HealthConstants {

  public static final String STATUS_OK = "ok";
  public static final String STATUS_UNAVAILABLE = "unavailable";
  public static final String MSG_NOT_READY = "readiness check failed";

  private static final int READY_TIMEOUT_SECONDS = 2;

  /**
   * How long a readiness probe waits on the database before reporting the instance unavailable.
   *
   * <p>A probe that waits as long as the database takes turns a slow dependency into a hung probe,
   * and the platform then keeps routing traffic to an instance that cannot serve it.
   */
  public static final Duration READY_TIMEOUT = Duration.ofSeconds(READY_TIMEOUT_SECONDS);

  private HealthConstants() {}
}
