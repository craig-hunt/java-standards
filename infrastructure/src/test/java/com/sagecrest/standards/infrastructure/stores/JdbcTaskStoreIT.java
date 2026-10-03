package com.sagecrest.standards.infrastructure.stores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.domain.errors.NotFoundException;
import com.sagecrest.standards.domain.tasks.TaskConstants;
import com.sagecrest.standards.domain.tasks.TaskId;
import com.sagecrest.standards.domain.tasks.TaskItem;
import com.sagecrest.standards.domain.tasks.TaskTitle;
import com.sagecrest.standards.infrastructure.persistence.PostgresFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JdbcTaskStoreIT extends PostgresFixture {

  private static final String FIRST = "Read the ADR";
  private static final String SECOND = "Answer the questionnaire";
  private static final String PADDED = "   Read the ADR   ";
  private static final long ABSENT_ID = 9_999L;
  private static final long ONE_DELETED = 1L;
  private static final long NOTHING_DELETED = 0L;
  private static final int TWO_ROWS = 2;

  private final JdbcTaskStore store = new JdbcTaskStore(database());

  @Test
  @DisplayName("hands back the identifier the database assigned, in the same round trip")
  void handsBackTheAssignedIdentifier() {
    TaskItem created = store.create(new TaskTitle(FIRST));

    assertThat(created.id().value()).isPositive();
    assertThat(created.title().value()).isEqualTo(FIRST);
    assertThat(created.completed()).isFalse();
  }

  @Test
  void listsTasksInIdentifierOrder() {
    store.create(new TaskTitle(FIRST));
    store.create(new TaskTitle(SECOND));

    assertThat(store.list())
        .extracting(task -> task.title().value())
        .containsExactly(FIRST, SECOND);
  }

  @Test
  @DisplayName("stores the trimmed title, because the domain type trimmed it before the insert")
  void storesTheTrimmedTitle() {
    store.create(new TaskTitle(PADDED));

    assertThat(store.list())
        .singleElement()
        .extracting(task -> task.title().value())
        .isEqualTo(FIRST);
  }

  @Test
  void marksATaskCompletedAndBackAgain() {
    TaskId id = store.create(new TaskTitle(FIRST)).id();

    assertThat(store.setCompleted(id, true).completed()).isTrue();
    assertThat(store.setCompleted(id, false).completed()).isFalse();
  }

  @Test
  @DisplayName("refuses to report success for an update that matched no row")
  void refusesAnUpdateThatMatchedNoRow() {
    assertThatThrownBy(() -> store.setCompleted(new TaskId(ABSENT_ID), true))
        .isInstanceOf(NotFoundException.class)
        .extracting(failure -> ((NotFoundException) failure).code())
        .isEqualTo(TaskConstants.CODE_NOT_FOUND);
  }

  @Test
  void deletesTheRowItWasAskedToDelete() {
    TaskId id = store.create(new TaskTitle(FIRST)).id();
    store.create(new TaskTitle(SECOND));

    store.delete(id);

    assertThat(store.list())
        .singleElement()
        .extracting(task -> task.title().value())
        .isEqualTo(SECOND);
  }

  @Test
  @DisplayName("refuses to report success for a delete that matched no row")
  void refusesADeleteThatMatchedNoRow() {
    assertThatThrownBy(() -> store.delete(new TaskId(ABSENT_ID)))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("reports how many completed rows it cleared, and leaves the rest")
  void reportsHowManyCompletedRowsItCleared() {
    TaskId done = store.create(new TaskTitle(FIRST)).id();
    store.create(new TaskTitle(SECOND));
    store.setCompleted(done, true);

    assertThat(store.deleteCompleted()).isEqualTo(ONE_DELETED);
    assertThat(store.deleteCompleted()).isEqualTo(NOTHING_DELETED);
    assertThat(store.list())
        .singleElement()
        .extracting(task -> task.title().value())
        .isEqualTo(SECOND);
  }

  @Test
  @DisplayName("isolates each test, so an earlier one cannot seed a later one")
  void isolatesEachTest() {
    store.create(new TaskTitle(FIRST));
    store.create(new TaskTitle(SECOND));

    assertThat(store.list()).hasSize(TWO_ROWS);
  }
}
