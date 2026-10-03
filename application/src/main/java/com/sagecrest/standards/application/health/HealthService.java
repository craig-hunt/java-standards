package com.sagecrest.standards.application.health;

import com.sagecrest.standards.application.ports.HealthProbe;
import com.sagecrest.standards.domain.health.HealthConstants;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs the readiness probe under a deadline.
 *
 * <p>The service reports the failure rather than logging it, so the logger stays a concern of the
 * layer that owns request context, and a test reads the outcome without capturing log output.
 *
 * <p>The executor arrives as a dependency. {@link ExecutorService} ships in the JDK, so taking one
 * here couples this module to the platform rather than to a framework, and it lets a test hand over
 * an executor it controls instead of racing a real clock.
 */
public final class HealthService {

  private static final boolean INTERRUPT_THE_PROBE = true;

  private final HealthProbe probe;
  private final ExecutorService probes;

  public HealthService(HealthProbe probe, ExecutorService probes) {
    this.probe = probe;
    this.probes = probes;
  }

  /**
   * Reports whether the dependency answered within the readiness timeout.
   *
   * <p>Every failure the probe raises reads as unavailable. Narrowing the catch would let an
   * unanticipated failure escape and answer a readiness request with a server error, which tells a
   * platform to keep routing traffic to an instance that cannot serve it.
   *
   * <p>The executor is deliberately not closed here, and deliberately not used with
   * try-with-resources. An {@code ExecutorService} closes by waiting for its tasks to finish, so a
   * probe wrapped that way would block for as long as the database took, precisely in the case the
   * timeout exists to cut short. The owner of the executor closes it when the process stops.
   */
  public ReadinessCheck check() {
    Future<?> attempt = probes.submit(probe::ping);
    try {
      attempt.get(HealthConstants.READY_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      return ReadinessCheck.available();
    } catch (TimeoutException tookTooLong) {
      attempt.cancel(INTERRUPT_THE_PROBE);
      return ReadinessCheck.unavailable(tookTooLong);
    } catch (ExecutionException failed) {
      return ReadinessCheck.unavailable(failed.getCause());
    } catch (InterruptedException interrupted) {
      // Cancel first. Returning without it would leave the probe running on its own
      // thread, still holding the connection it borrowed, after the caller has given
      // up waiting for the answer.
      attempt.cancel(INTERRUPT_THE_PROBE);
      // Then restore the flag, which is what lets the caller up the stack notice the
      // interrupt. Swallowing it strands a shutdown signal here.
      Thread.currentThread().interrupt();
      return ReadinessCheck.unavailable(interrupted);
    }
  }
}
