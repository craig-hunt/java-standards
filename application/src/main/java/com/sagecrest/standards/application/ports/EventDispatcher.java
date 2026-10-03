package com.sagecrest.standards.application.ports;

import com.sagecrest.standards.domain.events.DomainEvent;

/** Delivers an event to the consumers that want it. */
public interface EventDispatcher {

  void dispatch(DomainEvent event);
}
