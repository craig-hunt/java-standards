package com.sagecrest.standards.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DatabaseIT extends PostgresFixture {

  private static final String TITLE = "Read the ADR";
  private static final String SQL_COUNT_TASKS = "SELECT count(*) FROM tasks";
  private static final String BROKEN_SQL = "SELECT * FROM a_table_that_does_not_exist";
  private static final String DELIBERATE = "a deliberate failure after the insert";
  private static final int FIRST_COLUMN = 1;
  private static final long NO_ROWS = 0L;
  private static final long ONE_ROW = 1L;

  @Test
  @DisplayName("commits the work when it completes")
  void commitsCompletedWork() {
    database()
        .inTransaction(
            connection -> {
              insertTask(connection);
              return null;
            });

    assertThat(countTasks()).isEqualTo(ONE_ROW);
  }

  @Test
  @DisplayName("rolls back everything when the work throws partway through")
  void rollsBackPartialWork() {
    assertThatThrownBy(
            () ->
                database()
                    .inTransaction(
                        connection -> {
                          insertTask(connection);
                          throw new IllegalStateException(DELIBERATE);
                        }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(DELIBERATE);

    assertThat(countTasks()).isEqualTo(NO_ROWS);
  }

  @Test
  @DisplayName("rolls back when the database itself refuses a statement")
  void rollsBackOnADatabaseFailure() {
    assertThatThrownBy(
            () ->
                database()
                    .inTransaction(
                        connection -> {
                          insertTask(connection);
                          try (Statement broken = connection.createStatement()) {
                            broken.execute(BROKEN_SQL);
                          }
                          return null;
                        }))
        .isInstanceOf(InfrastructureException.class)
        .hasMessage(InfrastructureConstants.MSG_TRANSACTION_FAILED);

    assertThat(countTasks()).isEqualTo(NO_ROWS);
  }

  @Test
  @DisplayName("wraps a read failure with its own message, so the two paths stay distinguishable")
  void wrapsAReadFailure() {
    assertThatThrownBy(
            () ->
                database()
                    .query(
                        connection -> {
                          try (Statement broken = connection.createStatement()) {
                            return broken.execute(BROKEN_SQL);
                          }
                        }))
        .isInstanceOf(InfrastructureException.class)
        .hasMessage(InfrastructureConstants.MSG_QUERY_FAILED);
  }

  private static void insertTask(java.sql.Connection connection) throws java.sql.SQLException {
    try (PreparedStatement insert =
        connection.prepareStatement(InfrastructureConstants.SQL_INSERT_TASK)) {
      insert.setString(Columns.FIRST, TITLE);
      insert.executeQuery();
    }
  }

  private static long countTasks() {
    return database()
        .query(
            connection -> {
              try (PreparedStatement count = connection.prepareStatement(SQL_COUNT_TASKS);
                  ResultSet rows = count.executeQuery()) {
                rows.next();
                return rows.getLong(FIRST_COLUMN);
              }
            });
  }
}
