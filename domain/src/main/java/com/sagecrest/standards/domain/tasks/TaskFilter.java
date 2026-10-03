package com.sagecrest.standards.domain.tasks;

import java.util.List;

/**
 * Which tasks a list request shows.
 *
 * <p>Each constant carries the query value that names it, and the parse walks the constants, so
 * adding a filter cannot leave the parse behind. The C# sibling spells the same thing as extension
 * methods over a bare enum, where a new member falls silently into the default arm of both the
 * parse and the predicate. Here the switch in {@link #includes} lists every constant with no
 * default, so the compiler refuses to build until a new filter says which tasks it shows.
 */
public enum TaskFilter {
  ALL(TaskConstants.FILTER_ALL),
  ACTIVE(TaskConstants.FILTER_ACTIVE),
  COMPLETED(TaskConstants.FILTER_COMPLETED);

  private static final List<TaskFilter> CONSTANTS = List.of(values());

  private final String queryValue;

  TaskFilter(String queryValue) {
    this.queryValue = queryValue;
  }

  public String queryValue() {
    return queryValue;
  }

  /** Parses the query value, treating an absent value as {@link #ALL}. */
  public static TaskFilter parse(String raw) {
    if (raw == null || raw.isEmpty()) {
      return ALL;
    }
    return CONSTANTS.stream()
        .filter(filter -> filter.queryValue.equals(raw))
        .findFirst()
        .orElseThrow(TaskErrors::invalidFilter);
  }

  public boolean includes(TaskItem task) {
    return switch (this) {
      case ACTIVE -> !task.completed();
      case COMPLETED -> task.completed();
      case ALL -> true;
    };
  }
}
