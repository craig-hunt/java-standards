package com.sagecrest.standards.infrastructure.stores;

import com.sagecrest.standards.application.ports.TaskStore;
import com.sagecrest.standards.domain.tasks.TaskErrors;
import com.sagecrest.standards.domain.tasks.TaskId;
import com.sagecrest.standards.domain.tasks.TaskItem;
import com.sagecrest.standards.domain.tasks.TaskTitle;
import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import com.sagecrest.standards.infrastructure.persistence.Columns;
import com.sagecrest.standards.infrastructure.persistence.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
 */
public final class JdbcTaskStore implements TaskStore {

  private final Database database;

  public JdbcTaskStore(Database database) {
    this.database = database;
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

  @Override
  public TaskItem setCompleted(TaskId id, boolean completed) {
    return database.inTransaction(
        connection -> {
          try (PreparedStatement update =
              connection.prepareStatement(InfrastructureConstants.SQL_SET_TASK_COMPLETED)) {
            update.setBoolean(Columns.FIRST, completed);
            update.setLong(Columns.SECOND, id.value());
            return single(update);
          }
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

  private static final int NOTHING_CHANGED = 0;

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
