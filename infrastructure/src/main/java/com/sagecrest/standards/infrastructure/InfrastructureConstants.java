package com.sagecrest.standards.infrastructure;

/**
 * Every literal this module carries, named once: the SQL it runs, the columns it reads, and the
 * messages it logs.
 *
 * <p>SQL belongs here rather than inline for the same reason any other literal does. A statement
 * spelled twice drifts, and a column name typed into three stores fails at runtime in whichever one
 * nobody exercised.
 */
public final class InfrastructureConstants {

  public static final int OUTBOX_BATCH_SIZE = 50;

  public static final String COLUMN_ID = "id";
  public static final String COLUMN_TITLE = "title";
  public static final String COLUMN_COMPLETED = "completed";
  public static final String COLUMN_NAME = "name";
  public static final String COLUMN_QUANTITY = "quantity";
  public static final String COLUMN_STATUS = "status";
  public static final String COLUMN_EVENT_ID = "event_id";
  public static final String COLUMN_TYPE = "type";
  public static final String COLUMN_PAYLOAD = "payload";
  public static final String COLUMN_OCCURRED_AT = "occurred_at";
  public static final String COLUMN_WAS_COMPLETED = "was_completed";

  public static final String SQL_LIST_TASKS = "SELECT id, title, completed FROM tasks ORDER BY id";

  public static final String SQL_INSERT_TASK =
      "INSERT INTO tasks (title, completed) VALUES (?, false) RETURNING id, title, completed";

  /**
   * Sets a task's completion and reports what it was beforehand.
   *
   * <p>The prior value decides whether an event is due, and reading it in a separate statement
   * would leave a window in which another transaction changed it. The {@code FOR UPDATE} in the
   * common table expression takes the row lock before the update, so the before-and-after pair this
   * returns describes one atomic step.
   */
  public static final String SQL_SET_TASK_COMPLETED =
      """
      WITH previous AS (
        SELECT id, completed FROM tasks WHERE id = ? FOR UPDATE
      )
      UPDATE tasks
      SET completed = ?
      FROM previous
      WHERE tasks.id = previous.id
      RETURNING tasks.id, tasks.title, tasks.completed, previous.completed AS was_completed\
      """;

  public static final String SQL_DELETE_TASK = "DELETE FROM tasks WHERE id = ?";

  public static final String SQL_DELETE_COMPLETED_TASKS = "DELETE FROM tasks WHERE completed";

  public static final String SQL_INSERT_SIGNUP =
      """
      INSERT INTO signups (full_name, email, plan, seats, notes, created_at)
      VALUES (?, ?, ?, ?, ?, ?)
      RETURNING id\
      """;

  public static final String SQL_LIST_INVENTORY =
      "SELECT name, quantity, status FROM inventory ORDER BY name";

  public static final String SQL_INSERT_OUTBOX =
      """
      INSERT INTO outbox (event_id, type, payload, occurred_at)
      VALUES (?, ?, ?::jsonb, ?)\
      """;

  /**
   * Claims a batch of undelivered messages.
   *
   * <p>{@code FOR UPDATE SKIP LOCKED} is the whole point. A second replica running the same relay
   * takes a different batch rather than the same rows; without it, two relays read identical rows
   * and every consumer sees each message twice. At-least-once delivery tolerates a repeat, but that
   * is no reason to manufacture one on every pass.
   *
   * <p>{@code LIMIT} precedes {@code FOR UPDATE} because PostgreSQL requires that order.
   */
  public static final String SQL_CLAIM_OUTBOX =
      """
      SELECT event_id, type, payload, occurred_at
      FROM outbox
      WHERE published_at IS NULL
      ORDER BY occurred_at
      LIMIT ?
      FOR UPDATE SKIP LOCKED\
      """;

  public static final String SQL_MARK_PUBLISHED =
      "UPDATE outbox SET published_at = ? WHERE event_id = ?";

  public static final String SQL_PING = "SELECT 1";

  public static final String MSG_TRANSACTION_FAILED = "the database transaction did not complete";
  public static final String MSG_QUERY_FAILED = "the database query did not complete";
  public static final String MSG_ROW_VANISHED =
      "the row the statement should have returned is absent";
  public static final String MSG_OUTBOX_UNKNOWN_TYPE =
      "outbox message {} names a type this deployment cannot resolve: {}";
  public static final String MSG_SERIALIZE_FAILED = "the event could not be written as JSON";
  public static final String MSG_SIGNUP_RECORDED = "signup {} took the {} plan with {} seats";
  public static final String MSG_TASK_COMPLETED = "task {} was completed: {}";
  public static final String MSG_RELAY_FAILED = "the outbox relay pass did not complete";

  private InfrastructureConstants() {}
}
