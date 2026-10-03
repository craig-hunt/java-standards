package com.sagecrest.standards.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Something the domain recorded as having happened.
 *
 * <p>Every payload carries primitives rather than domain types. An event outlives the process that
 * raised it and may reach a consumer that shares no code with this service, so its wire shape stays
 * independent of the types in here. A typed identifier protects calls inside this process; it would
 * only couple a reader outside it.
 *
 * <p>Sealed, so any reader that switches over the events gets told by the compiler when a new one
 * arrives unhandled.
 */
public sealed interface DomainEvent permits SignupRecorded, TaskCompleted {

  UUID eventId();

  Instant occurredAt();
}
