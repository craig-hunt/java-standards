package com.sagecrest.standards.application.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.application.fakes.RecordingTaskStore;
import com.sagecrest.standards.domain.tasks.TaskFilter;
import com.sagecrest.standards.domain.tasks.TaskId;
import com.sagecrest.standards.domain.tasks.TaskItem;
import com.sagecrest.standards.domain.tasks.TaskTitle;
import com.sagecrest.standards.domain.tasks.TaskView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TaskServiceTest {

  private static final String FIRST = "Read the ADR";
  private static final String SECOND = "Answer the questionnaire";
  private static final String THIRD = "Book the review";
  private static final String NEW_TITLE = "Draft the runbook";

  private static final long FIRST_ID = 1L;
  private static final long SECOND_ID = 2L;
  private static final long THIRD_ID = 3L;

  private static final int TOTAL = 3;
  private static final int ACTIVE = 2;
  private static final int COMPLETED = 1;
  private static final long ONE_CLEARED = 1L;
  private static final long NOTHING_CLEARED = 0L;

  private RecordingTaskStore store;
  private TaskService service;

  @BeforeEach
  void setUp() {
    store = new RecordingTaskStore();
    service = new TaskService(store);
    store.given(
        new TaskItem(new TaskId(FIRST_ID), new TaskTitle(FIRST), false),
        new TaskItem(new TaskId(SECOND_ID), new TaskTitle(SECOND), true),
        new TaskItem(new TaskId(THIRD_ID), new TaskTitle(THIRD), false));
  }

  @Test
  @DisplayName("counts across every task even when the filter narrows the view")
  void countsAcrossEveryTask() {
    TaskView view = service.list(TaskFilter.COMPLETED);

    assertThat(view.tasks()).hasSize(COMPLETED);
    assertThat(view.remaining()).isEqualTo(ACTIVE);
    assertThat(view.total()).isEqualTo(TOTAL);
  }

  @Test
  void showsTheTasksTheFilterNames() {
    assertThat(service.list(TaskFilter.ACTIVE).tasks())
        .extracting(task -> task.title().value())
        .containsExactly(FIRST, THIRD);
  }

  @Test
  @DisplayName("creates a task that starts out incomplete")
  void createsAnIncompleteTask() {
    TaskItem created = service.create(new TaskTitle(NEW_TITLE));

    assertThat(created.title().value()).isEqualTo(NEW_TITLE);
    assertThat(created.completed()).isFalse();
    assertThat(service.list(TaskFilter.ALL).total()).isEqualTo(TOTAL + 1);
  }

  @Test
  void marksATaskCompletedAndBackAgain() {
    assertThat(service.setCompleted(new TaskId(FIRST_ID), true).completed()).isTrue();
    assertThat(service.setCompleted(new TaskId(FIRST_ID), false).completed()).isFalse();
  }

  @Test
  void deletesTheTaskItWasAskedToDelete() {
    service.delete(new TaskId(SECOND_ID));

    assertThat(store.deleted()).containsExactly(new TaskId(SECOND_ID));
    assertThat(service.list(TaskFilter.ALL).total()).isEqualTo(ACTIVE);
  }

  @Test
  @DisplayName("reports how many completed tasks it cleared")
  void reportsHowManyItCleared() {
    assertThat(service.clearCompleted()).isEqualTo(ONE_CLEARED);
    assertThat(service.clearCompleted()).isEqualTo(NOTHING_CLEARED);
    assertThat(service.list(TaskFilter.ALL).total()).isEqualTo(ACTIVE);
  }
}
