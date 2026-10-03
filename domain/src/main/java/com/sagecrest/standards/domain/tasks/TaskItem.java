package com.sagecrest.standards.domain.tasks;

import com.sagecrest.standards.domain.events.TaskCompleted;
import java.time.Instant;
import java.util.UUID;

/** One task, as the domain understands it. */
public record TaskItem(TaskId id, TaskTitle title, boolean completed) {

  /**
   * Describes this task's completion as the event a consumer receives.
   *
   * <p>The task composes its own event, for the same reason a signup does: the fact travels with
   * the type that holds the rule, rather than being assembled by whichever layer happens to write
   * the row. The clock arrives from the caller because the domain owns no clock.
   */
  public TaskCompleted completionRecorded(UUID eventId, Instant occurredAt) {
    return new TaskCompleted(eventId, occurredAt, id.value(), title.value());
  }
}
