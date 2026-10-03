package com.sagecrest.standards.application.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.application.ports.HealthProbe;
import com.sagecrest.standards.domain.health.HealthConstants;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HealthServiceTest {

  private static final String DATABASE_DOWN = "connection refused";

  private static final int ONE_ARRIVAL = 1;
  private static final int FAR_LONGER = 10;

  /** Long enough that the probe cannot finish on its own before the deadline cuts it off. */
  private static final Duration OUTLASTS_THE_TIMEOUT =
      HealthConstants.READY_TIMEOUT.multipliedBy(FAR_LONGER);

  private ExecutorService probes;

  @BeforeEach
  void setUp() {
    probes = Executors.newVirtualThreadPerTaskExecutor();
  }

  @AfterEach
  void tearDown() {
    probes.shutdownNow();
  }

  @Test
  void reportsReadyWhenTheDependencyAnswers() {
    ReadinessCheck outcome = new HealthService(() -> {}, probes).check();

    assertThat(outcome.ready()).isTrue();
    assertThat(outcome.failure()).isEmpty();
  }

  @Test
  @DisplayName("reports the failure the probe raised, not the wrapper the executor added")
  void reportsTheFailureTheProbeRaised() {
    IllegalStateException refused = new IllegalStateException(DATABASE_DOWN);
    HealthProbe failing =
        () -> {
          throw refused;
        };

    ReadinessCheck outcome = new HealthService(failing, probes).check();

    assertThat(outcome.ready()).isFalse();
    assertThat(outcome.failure()).containsSame(refused);
  }

  @Test
  @DisplayName(
      "abandons the probe when its own thread is interrupted, rather than leaving it to run")
  void abandonsTheProbeWhenInterrupted() throws InterruptedException {
    // One thread, already occupied, so the probe is still queued when the caller gives
    // up. A probe that had already started would make this a race: cancellation would
    // sometimes interrupt a running task and sometimes discard a queued one, and the
    // test would assert whichever happened that time.
    ExecutorService single = Executors.newSingleThreadExecutor();
    CountDownLatch occupied = new CountDownLatch(ONE_ARRIVAL);
    CountDownLatch release = new CountDownLatch(ONE_ARRIVAL);
    AtomicBoolean probeRan = new AtomicBoolean(false);

    single.submit(
        () -> {
          occupied.countDown();
          try {
            release.await();
          } catch (InterruptedException cutShort) {
            Thread.currentThread().interrupt();
          }
        });
    assertThat(occupied.await(HealthConstants.READY_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
        .isTrue();

    ReadinessCheck outcome;
    Thread.currentThread().interrupt();
    try {
      outcome = new HealthService(() -> probeRan.set(true), single).check();
    } finally {
      // Clearing it here rather than leaving it for the next test, which would
      // otherwise inherit an interrupt it never asked for.
      Thread.interrupted();
    }

    release.countDown();
    single.shutdown();
    assertThat(
            single.awaitTermination(
                HealthConstants.READY_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
        .isTrue();

    assertThat(outcome.ready()).isFalse();
    assertThat(outcome.failure()).get().isInstanceOf(InterruptedException.class);
    assertThat(probeRan)
        .as("an abandoned probe must not run later and borrow a connection nobody is waiting on")
        .isFalse();
  }

  @Test
  @DisplayName("restores the interrupt flag, so a shutdown signal is not stranded here")
  void restoresTheInterruptFlag() {
    HealthProbe hanging =
        () -> {
          try {
            Thread.sleep(OUTLASTS_THE_TIMEOUT);
          } catch (InterruptedException cutShort) {
            Thread.currentThread().interrupt();
          }
        };

    Thread.currentThread().interrupt();
    try {
      new HealthService(hanging, probes).check();
      assertThat(Thread.currentThread().isInterrupted())
          .as("the caller up the stack still needs to see the interrupt")
          .isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  @DisplayName("gives up on a probe that outlasts the readiness timeout, and interrupts it")
  void givesUpOnAProbeThatOutlastsTheTimeout() throws InterruptedException {
    CountDownLatch cancelled = new CountDownLatch(ONE_ARRIVAL);
    HealthProbe hanging =
        () -> {
          try {
            Thread.sleep(OUTLASTS_THE_TIMEOUT);
          } catch (InterruptedException cutShort) {
            cancelled.countDown();
            Thread.currentThread().interrupt();
          }
        };

    ReadinessCheck outcome = new HealthService(hanging, probes).check();

    assertThat(outcome.ready()).isFalse();
    assertThat(outcome.failure()).isPresent();
    assertThat(cancelled.await(HealthConstants.READY_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
        .as("the probe thread was interrupted rather than left running")
        .isTrue();
  }
}
