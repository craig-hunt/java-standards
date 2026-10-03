package com.sagecrest.standards.domain.events;

import java.time.Instant;
import java.util.UUID;

/** A validated signup reached the database. */
public record SignupRecorded(
    UUID eventId, Instant occurredAt, long signupId, String email, String plan, int seats)
    implements DomainEvent {}
