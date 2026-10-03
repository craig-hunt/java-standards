package com.sagecrest.standards.infrastructure.events;

import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wakes on a timer and asks the publisher to drain the outbox.
 *
 * <p>It catches everything a pass can raise. An exception that escaped a scheduled task would
 * cancel the schedule silently, and the service would keep serving requests while its backlog grew
 * with nobody draining it. A failed pass gets logged, and the next tick tries again.
 *
 * <p>{@code scheduleWithFixedDelay} rather than {@code scheduleAtFixedRate}: a pass that outran the
 * interval would otherwise have the next pass queued behind it immediately, and a slow database
 * would turn into a pile-up.
 */
public final class OutboxRelay implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);

  private final OutboxPublisher publisher;
  private final ScheduledExecutorService schedule;
  private final Duration interval;

  public OutboxRelay(
      OutboxPublisher publisher, ScheduledExecutorService schedule, Duration interval) {
    this.publisher = publisher;
    this.schedule = schedule;
    this.interval = interval;
  }

  public void start() {
    schedule.scheduleWithFixedDelay(
        this::drain, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
  }

  private void drain() {
    try {
      publisher.publishPending();
    } catch (RuntimeException failed) {
      LOG.error(InfrastructureConstants.MSG_RELAY_FAILED, failed);
    }
  }

  @Override
  public void close() {
    schedule.shutdownNow();
  }
}
