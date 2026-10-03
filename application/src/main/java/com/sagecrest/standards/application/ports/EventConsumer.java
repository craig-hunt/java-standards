package com.sagecrest.standards.application.ports;

import com.sagecrest.standards.domain.events.DomainEvent;

/**
 * Reacts to an event the service published.
 *
 * <p>A consumer must absorb repeats. The outbox delivers at least once: a relay can publish a
 * message and fail before recording that it did, and the next pass sends it again. A consumer that
 * assumes exactly-once delivery will double-charge, double-mail, or double-count the first time
 * that happens.
 */
public interface EventConsumer {

  boolean accepts(DomainEvent event);

  void consume(DomainEvent event);
}
