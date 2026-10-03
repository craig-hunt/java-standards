package com.sagecrest.standards.domain.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.domain.events.TaskCompleted;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TaskItemTest {

  private static final long ID = 4L;
  private static final String TITLE = "Read the ADR";

  @Test
  @DisplayName("composes its own event, carrying primitives rather than domain types")
  void composesItsOwnEvent() {
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();
    TaskItem task = new TaskItem(new TaskId(ID), new TaskTitle(TITLE), true);

    TaskCompleted recorded = task.completionRecorded(eventId, occurredAt);

    assertThat(recorded.eventId()).isEqualTo(eventId);
    assertThat(recorded.occurredAt()).isEqualTo(occurredAt);
    assertThat(recorded.taskId()).isEqualTo(ID);
    assertThat(recorded.title()).isEqualTo(TITLE);
  }
}
