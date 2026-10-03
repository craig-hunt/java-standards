package com.sagecrest.standards.application.events;

import com.sagecrest.standards.application.ports.EventConsumer;
import com.sagecrest.standards.application.ports.EventDispatcher;
import com.sagecrest.standards.domain.events.DomainEvent;
import java.util.List;

/**
 * Hands an event to every consumer that accepts it.
 *
 * <p>This is the whole of what a mediator library would sell for this job. Dispatch costs a loop
 * and an interface rather than a dependency with its own release cadence, its own annotations, and
 * its own license.
 */
public final class FanOutEventDispatcher implements EventDispatcher {

  private final List<EventConsumer> consumers;

  public FanOutEventDispatcher(List<EventConsumer> consumers) {
    this.consumers = List.copyOf(consumers);
  }

  @Override
  public void dispatch(DomainEvent event) {
    for (EventConsumer consumer : consumers) {
      if (consumer.accepts(event)) {
        consumer.consume(event);
      }
    }
  }
}
