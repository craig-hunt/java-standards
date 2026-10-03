package com.sagecrest.standards.application.events;

import com.sagecrest.standards.application.ports.EventConsumer;
import com.sagecrest.standards.domain.events.DomainEvent;

/**
 * A consumer of one event type.
 *
 * <p>The untyped {@link EventConsumer} lets the dispatcher hold every consumer in one list; this
 * base restores the typed signature at the consuming end.
 *
 * <p>The class token is not ceremony. The C# sibling writes {@code domainEvent is TEvent} because
 * .NET generics survive to runtime. Java erases them, so a subclass states its event type once, as
 * a constructor argument, rather than every dispatch site reaching for reflection to recover what
 * the compiler threw away.
 *
 * <p>Erasure also decides the name below. A protected {@code consume(E)} beside the public {@code
 * consume(DomainEvent)} does not compile: both erase to {@code consume(DomainEvent)}, so the
 * compiler reads them as one method declared twice. The typed half is called {@code handle} for
 * that reason and no other.
 *
 * @param <E> the event type this consumer accepts
 */
public abstract class DomainEventConsumer<E extends DomainEvent> implements EventConsumer {

  private final Class<E> accepted;

  protected DomainEventConsumer(Class<E> accepted) {
    this.accepted = accepted;
  }

  @Override
  public final boolean accepts(DomainEvent event) {
    return accepted.isInstance(event);
  }

  @Override
  public final void consume(DomainEvent event) {
    handle(accepted.cast(event));
  }

  protected abstract void handle(E event);
}
