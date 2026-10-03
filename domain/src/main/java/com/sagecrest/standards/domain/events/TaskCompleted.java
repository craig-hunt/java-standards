package com.sagecrest.standards.domain.events;

import java.time.Instant;
import java.util.UUID;

/** A task was marked completed. */
public record TaskCompleted(UUID eventId, Instant occurredAt, long taskId, String title)
    implements DomainEvent {}
