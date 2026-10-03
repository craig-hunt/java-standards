package com.sagecrest.standards.infrastructure.stores;

import com.sagecrest.standards.application.ports.TaskStore;
import com.sagecrest.standards.domain.tasks.TaskErrors;
import com.sagecrest.standards.domain.tasks.TaskId;
import com.sagecrest.standards.domain.tasks.TaskItem;
import com.sagecrest.standards.domain.tasks.TaskTitle;
import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import com.sagecrest.standards.infrastructure.events.OutboxWriter;
import com.sagecrest.standards.infrastructure.persistence.Columns;
import com.sagecrest.standards.infrastructure.persistence.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tasks, in PostgreSQL.
 *
 * <p>No row type sits between the table and the domain. The C# sibling needs one because EF tracks
 * changes on mutable entities and an insert leaves an entity half-built while the database assigns
 * a key. JDBC hands back a result set, and {@code RETURNING} hands back the assigned key in the
 * same round trip, so the mapping reads straight into the domain type and a middle layer would
 * carry no information.
 *
 * <p>A write that matched no row raises the domain's not-found failure rather than answering
 * quietly. A silent no-op there is how a delete appears to succeed against an id that never
 * existed.
 *
 * <p>Completing a task writes the task row and the event announcing it in one transaction, for the
 * same reason a signup does. Announcing it afterwards, outside the transaction, would let a crash
 * between the two leave a completed task nobody was told about.
 */
public final class JdbcTaskStore implements TaskStore {

  private static final int NOTHING_CHANGED = 0;

  private final Database database;
  private final Clock clock;

  public JdbcTaskStore(Database database, Clock clock) {
    this.database = database;
    this.clock = clock;
  }

  @Override
  public List<TaskItem> list() {
    return database.query(
        connection -> {
          try (PreparedStatement query =
                  connection.prepareStatement(InfrastructureConstants.SQL_LIST_TASKS);
              ResultSet rows = query.executeQuery()) {
            List<TaskItem> tasks = new ArrayList<>();
            while (rows.next()) {
              tasks.add(read(rows));
            }
            return List.copyOf(tasks);
          }
        });
  }

  @Override
  public TaskItem create(TaskTitle title) {
    return database.inTransaction(
        connection -> {
          try (PreparedStatement insert =
              connection.prepareStatement(InfrastructureConstants.SQL_INSERT_TASK)) {
            insert.setString(Columns.FIRST, title.value());
            return single(insert);
          }
        });
  }

  /**
   * Sets completion, and announces it when a task becomes completed.
   *
   * <p>The event fires on the transition only. Setting a completed task completed again announces
   * nothing: the outbox already tolerates a repeat, but manufacturing one on every idempotent
   * request would make the backlog grow with messages that say nothing new. Marking a task
   * incomplete announces nothing either, because no event here describes that.
   */
  @Override
  public TaskItem setCompleted(TaskId id, boolean completed) {
    return database.inTransaction(
        connection -> {
          Completion outcome = update(connection, id, completed);
          if (outcome.becameCompleted()) {
            OutboxWriter.write(
                connection, outcome.task().completionRecorded(UUID.randomUUID(), clock.instant()));
          }
          return outcome.task();
        });
  }

  @Override
  public void delete(TaskId id) {
    database.inTransaction(
        connection -> {
          try (PreparedStatement delete =
              connection.prepareStatement(InfrastructureConstants.SQL_DELETE_TASK)) {
            delete.setLong(Columns.FIRST, id.value());
            if (delete.executeUpdate() == NOTHING_CHANGED) {
              throw TaskErrors.notFound();
            }
            return null;
          }
        });
  }

  @Override
  public long deleteCompleted() {
    return database.inTransaction(
        connection -> {
          try (PreparedStatement delete =
              connection.prepareStatement(InfrastructureConstants.SQL_DELETE_COMPLETED_TASKS)) {
            return (long) delete.executeUpdate();
          }
        });
  }

  /** The updated task, and whether this update is what completed it. */
  private record Completion(TaskItem task, boolean wasCompleted) {

    boolean becameCompleted() {
      return task.completed() && !wasCompleted;
    }
  }

  private static Completion update(Connection connection, TaskId id, boolean completed)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(InfrastructureConstants.SQL_SET_TASK_COMPLETED)) {
      statement.setLong(Columns.FIRST, id.value());
      statement.setBoolean(Columns.SECOND, completed);
      try (ResultSet rows = statement.executeQuery()) {
        if (!rows.next()) {
          throw TaskErrors.notFound();
        }
        return new Completion(
            read(rows), rows.getBoolean(InfrastructureConstants.COLUMN_WAS_COMPLETED));
      }
    }
  }

  private static TaskItem single(PreparedStatement statement) throws SQLException {
    try (ResultSet rows = statement.executeQuery()) {
      if (!rows.next()) {
        throw TaskErrors.notFound();
      }
      return read(rows);
    }
  }

  private static TaskItem read(ResultSet rows) throws SQLException {
    return new TaskItem(
        new TaskId(rows.getLong(InfrastructureConstants.COLUMN_ID)),
        new TaskTitle(rows.getString(InfrastructureConstants.COLUMN_TITLE)),
        rows.getBoolean(InfrastructureConstants.COLUMN_COMPLETED));
  }
}
