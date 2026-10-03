package com.sagecrest.standards.domain.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.domain.errors.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class TaskFilterTest {

  private static final long FIRST_ID = 1L;
  private static final String ANY_TITLE = "Anything";
  private static final String UNKNOWN = "archived";

  private static final TaskItem ACTIVE_TASK =
      new TaskItem(new TaskId(FIRST_ID), new TaskTitle(ANY_TITLE), false);
  private static final TaskItem COMPLETED_TASK =
      new TaskItem(new TaskId(FIRST_ID), new TaskTitle(ANY_TITLE), true);

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("treats an absent filter as showing everything")
  void treatsAnAbsentFilterAsAll(String raw) {
    assertThat(TaskFilter.parse(raw)).isEqualTo(TaskFilter.ALL);
  }

  @ParameterizedTest
  @EnumSource(TaskFilter.class)
  @DisplayName("parses the query value every constant publishes")
  void parsesEveryQueryValueItPublishes(TaskFilter filter) {
    assertThat(TaskFilter.parse(filter.queryValue())).isEqualTo(filter);
  }

  @Test
  void rejectsAFilterNameItDoesNotPublish() {
    assertThatThrownBy(() -> TaskFilter.parse(UNKNOWN))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).code())
        .isEqualTo(TaskConstants.CODE_INVALID_FILTER);
  }

  @Test
  @DisplayName("each filter shows the tasks it names and hides the others")
  void showsOnlyTheTasksItNames() {
    assertThat(TaskFilter.ALL.includes(ACTIVE_TASK)).isTrue();
    assertThat(TaskFilter.ALL.includes(COMPLETED_TASK)).isTrue();
    assertThat(TaskFilter.ACTIVE.includes(ACTIVE_TASK)).isTrue();
    assertThat(TaskFilter.ACTIVE.includes(COMPLETED_TASK)).isFalse();
    assertThat(TaskFilter.COMPLETED.includes(COMPLETED_TASK)).isTrue();
    assertThat(TaskFilter.COMPLETED.includes(ACTIVE_TASK)).isFalse();
  }
}
