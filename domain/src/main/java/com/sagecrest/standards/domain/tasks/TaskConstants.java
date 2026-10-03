package com.sagecrest.standards.domain.tasks;

/**
 * Every literal the task feature carries, named once. Tests reference these rather than repeating
 * the values, so a reworded message fails at compile time instead of drifting.
 */
public final class TaskConstants {

  public static final int MAX_TITLE_LENGTH = 200;

  public static final String FILTER_ALL = "all";
  public static final String FILTER_ACTIVE = "active";
  public static final String FILTER_COMPLETED = "completed";

  public static final String FIELD_TITLE = "title";
  public static final String FIELD_COMPLETED = "completed";

  public static final String CODE_INVALID_FILTER = "invalid_filter";
  public static final String CODE_INVALID_ID = "invalid_id";
  public static final String CODE_NOT_FOUND = "not_found";

  public static final String MSG_INVALID_FILTER = "filter must be all, active, or completed";
  public static final String MSG_INVALID_ID = "task id must be a positive whole number";
  public static final String MSG_NOT_FOUND = "no task has that id";
  public static final String MSG_TITLE_REQUIRED = "Enter a task title.";
  public static final String MSG_TITLE_TOO_LONG = "Keep the task title within the length limit.";
  public static final String MSG_COMPLETED_REQUIRED = "Say whether the task is completed.";

  public static final String SEED_TITLE_FIRST = "Review the architecture decision record";
  public static final String SEED_TITLE_SECOND = "Reply to the vendor questionnaire";

  private TaskConstants() {}
}
