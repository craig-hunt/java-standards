package com.sagecrest.standards.infrastructure.stores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.domain.errors.NotFoundException;
import com.sagecrest.standards.domain.tasks.TaskConstants;
import com.sagecrest.standards.domain.tasks.TaskId;
import com.sagecrest.standards.domain.tasks.TaskItem;
import com.sagecrest.standards.domain.tasks.TaskTitle;
import com.sagecrest.standards.infrastructure.persistence.PostgresFixture;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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

  private static final String SQL_READ_OUTBOX =
      "SELECT type, payload, occurred_at FROM outbox ORDER BY occurred_at";
  private static final String COLUMN_TYPE = "type";
  private static final String COLUMN_PAYLOAD = "payload";
  private static final String COLUMN_OCCURRED_AT = "occurred_at";
  private static final String EXPECTED_WIRE_NAME = "TaskCompleted";

  private static final Instant COMPLETED_AT = Instant.parse("2026-10-03T14:45:12Z");

  private final JdbcTaskStore store =
      new JdbcTaskStore(database(), Clock.fixed(COMPLETED_AT, ZoneOffset.UTC));

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
  @DisplayName("announces a completion in the transaction that caused it")
  void announcesACompletion() {
    TaskId id = store.create(new TaskTitle(FIRST)).id();

    store.setCompleted(id, true);

    assertThat(outboxTypes()).containsExactly(EXPECTED_WIRE_NAME);
    assertThat(outboxPayload()).contains(FIRST, String.valueOf(id.value()));
  }

  @Test
  @DisplayName("announces nothing when a task was already completed")
  void announcesNothingOnARepeat() {
    TaskId id = store.create(new TaskTitle(FIRST)).id();
    store.setCompleted(id, true);

    store.setCompleted(id, true);

    assertThat(outboxTypes())
        .as("a repeated request says nothing new, so it adds nothing to the backlog")
        .containsExactly(EXPECTED_WIRE_NAME);
  }

  @Test
  @DisplayName("announces nothing when a task is reopened, because no event describes that")
  void announcesNothingOnReopening() {
    TaskId id = store.create(new TaskTitle(FIRST)).id();
    store.setCompleted(id, true);

    store.setCompleted(id, false);

    assertThat(outboxTypes()).containsExactly(EXPECTED_WIRE_NAME);
  }

  @Test
  @DisplayName("announces nothing for a task that was never completed")
  void announcesNothingWithoutACompletion() {
    store.create(new TaskTitle(FIRST));

    assertThat(outboxTypes()).isEmpty();
  }

  @Test
  @DisplayName("stamps the event with the injected clock rather than the wall clock")
  void stampsTheEventWithTheInjectedClock() {
    store.setCompleted(store.create(new TaskTitle(FIRST)).id(), true);

    assertThat(outboxOccurredAt()).isEqualTo(COMPLETED_AT);
  }

  @Test
  @DisplayName("isolates each test, so an earlier one cannot seed a later one")
  void isolatesEachTest() {
    store.create(new TaskTitle(FIRST));
    store.create(new TaskTitle(SECOND));

    assertThat(store.list()).hasSize(TWO_ROWS);
  }

  private static List<String> outboxTypes() {
    return database()
        .query(
            connection -> {
              try (PreparedStatement read = connection.prepareStatement(SQL_READ_OUTBOX);
                  ResultSet rows = read.executeQuery()) {
                List<String> types = new ArrayList<>();
                while (rows.next()) {
                  types.add(rows.getString(COLUMN_TYPE));
                }
                return List.copyOf(types);
              }
            });
  }

  private static String outboxPayload() {
    return readOutbox(rows -> rows.getString(COLUMN_PAYLOAD));
  }

  private static Instant outboxOccurredAt() {
    return readOutbox(rows -> rows.getObject(COLUMN_OCCURRED_AT, OffsetDateTime.class).toInstant());
  }

  private static <T> T readOutbox(OutboxRead<T> read) {
    return database()
        .query(
            connection -> {
              try (PreparedStatement query = connection.prepareStatement(SQL_READ_OUTBOX);
                  ResultSet rows = query.executeQuery()) {
                rows.next();
                return read.from(rows);
              }
            });
  }

  @FunctionalInterface
  private interface OutboxRead<T> {
    T from(ResultSet rows) throws java.sql.SQLException;
  }
}
