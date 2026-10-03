package com.sagecrest.standards.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sagecrest.standards.domain.events.DomainEvent;
import com.sagecrest.standards.domain.events.SignupRecorded;
import com.sagecrest.standards.domain.events.TaskCompleted;
import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import com.sagecrest.standards.infrastructure.persistence.InfrastructureException;
import java.util.Map;
import java.util.Optional;

/**
 * Turns an event into an outbox row and back.
 *
 * <p>The stored type name is a literal, not {@code getClass().getSimpleName()}. A derived name
 * would change the moment somebody renamed a class, and every row written before the rename would
 * stop resolving: a refactor nobody thought of as a migration would silently strand a backlog.
 *
 * <p>Deserialization maps the stored name through a table rather than looking up a class by name.
 * Resolving whatever name a row happens to hold would let anything that can write a row choose a
 * class to construct.
 *
 * <p>The writing direction switches over the sealed event hierarchy with no default arm. That is
 * what sealing buys here: a new event type cannot reach the outbox without a wire name, because the
 * build stops until it has one.
 */
public final class OutboxSerializer {

  private static final String TYPE_SIGNUP_RECORDED = "SignupRecorded";
  private static final String TYPE_TASK_COMPLETED = "TaskCompleted";

  private static final Map<String, Class<? extends DomainEvent>> KNOWN =
      Map.of(
          TYPE_SIGNUP_RECORDED, SignupRecorded.class,
          TYPE_TASK_COMPLETED, TaskCompleted.class);

  private static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          // A timestamp written as a float loses precision and reads differently in every
          // language that consumes it. ISO-8601 text does not.
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();

  public static OutboxMessage toRow(DomainEvent event) {
    try {
      return new OutboxMessage(
          event.eventId(), wireName(event), MAPPER.writeValueAsString(event), event.occurredAt());
    } catch (JsonProcessingException unwritable) {
      throw new InfrastructureException(InfrastructureConstants.MSG_SERIALIZE_FAILED, unwritable);
    }
  }

  /** Answers empty when this deployment cannot resolve the stored type. */
  public static Optional<DomainEvent> fromRow(OutboxMessage row) {
    Class<? extends DomainEvent> type = KNOWN.get(row.type());
    if (type == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(MAPPER.readValue(row.payload(), type));
    } catch (JsonProcessingException unreadable) {
      return Optional.empty();
    }
  }

  private static String wireName(DomainEvent event) {
    return switch (event) {
      case SignupRecorded recorded -> TYPE_SIGNUP_RECORDED;
      case TaskCompleted completed -> TYPE_TASK_COMPLETED;
    };
  }

  private OutboxSerializer() {}
}
