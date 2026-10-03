package com.sagecrest.standards.domain.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TaskViewTest {

  private static final int TOTAL = 3;
  private static final int REMAINING = 2;
  private static final int ONE_SHOWN = 1;
  private static final int NONE = 0;

  private static final String FIRST = "First";
  private static final String SECOND = "Second";
  private static final String THIRD = "Third";

  private static final List<TaskItem> TASKS =
      List.of(
          new TaskItem(new TaskId(1L), new TaskTitle(FIRST), false),
          new TaskItem(new TaskId(2L), new TaskTitle(SECOND), true),
          new TaskItem(new TaskId(3L), new TaskTitle(THIRD), false));

  @Test
  @DisplayName("counts remaining and total across every task, not the filtered view")
  void countsAcrossEveryTask() {
    TaskView completedOnly = TaskView.summarize(TASKS, TaskFilter.COMPLETED);

    assertThat(completedOnly.tasks()).hasSize(ONE_SHOWN);
    assertThat(completedOnly.remaining()).isEqualTo(REMAINING);
    assertThat(completedOnly.total()).isEqualTo(TOTAL);
  }

  @Test
  void showsEveryTaskUnderTheAllFilter() {
    assertThat(TaskView.summarize(TASKS, TaskFilter.ALL).tasks()).hasSize(TOTAL);
  }

  @Test
  void showsTheIncompleteTasksUnderTheActiveFilter() {
    assertThat(TaskView.summarize(TASKS, TaskFilter.ACTIVE).tasks()).hasSize(REMAINING);
  }

  @Test
  @DisplayName("keeps the order the store returned")
  void keepsTheOrderTheStoreReturned() {
    List<TaskItem> shown = TaskView.summarize(TASKS, TaskFilter.ACTIVE).tasks();

    assertThat(shown).extracting(task -> task.title().value()).containsExactly(FIRST, THIRD);
  }

  @Test
  void reportsNothingForAnEmptySet() {
    TaskView empty = TaskView.summarize(List.of(), TaskFilter.ALL);

    assertThat(empty.tasks()).isEmpty();
    assertThat(empty.remaining()).isEqualTo(NONE);
    assertThat(empty.total()).isEqualTo(NONE);
  }

  @Test
  @DisplayName("hands back a list a caller cannot change underneath it")
  void handsBackAnUnmodifiableList() {
    List<TaskItem> shown = TaskView.summarize(TASKS, TaskFilter.ALL).tasks();

    assertThatThrownBy(shown::clear).isInstanceOf(UnsupportedOperationException.class);
  }
}
