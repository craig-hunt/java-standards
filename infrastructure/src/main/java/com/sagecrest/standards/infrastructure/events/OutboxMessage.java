package com.sagecrest.standards.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

/**
 * The persistence shape of an outbox message.
 *
 * <p>The row commits in the same transaction as the state change that produced it. That single fact
 * is what makes the outbox worth having: without it the service writes to the database and to a
 * broker separately, and any failure between the two leaves one of them wrong.
 *
 * <p>This is the one place a row type earns its keep. Elsewhere the stores map a result set
 * straight into a domain type, but a stored event is a wire format: it holds a type name and a JSON
 * string that no domain type should know about.
 */
public record OutboxMessage(UUID eventId, String type, String payload, Instant occurredAt) {}
