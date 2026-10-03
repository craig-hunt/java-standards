package com.sagecrest.standards.domain.tasks;

import com.sagecrest.standards.domain.errors.ErrorConstants;
import com.sagecrest.standards.domain.errors.NotFoundException;
import com.sagecrest.standards.domain.errors.ValidationException;
import java.util.Map;

/**
 * The failures the task feature raises, each with a name at the point it is thrown.
 *
 * <p>These live beside the feature rather than in the errors package, so the errors package stays
 * ignorant of which features exist. The C# sibling collects its per-feature exceptions into the
 * errors namespace, which forces that namespace to import every feature it describes; inverting the
 * direction here costs nothing and leaves one fewer edge in the dependency graph.
 */
public final class TaskErrors {

  public static ValidationException titleRequired() {
    return new ValidationException(
        ErrorConstants.CODE_VALIDATION,
        ErrorConstants.MSG_VALIDATION,
        Map.of(TaskConstants.FIELD_TITLE, TaskConstants.MSG_TITLE_REQUIRED));
  }

  public static ValidationException titleTooLong() {
    return new ValidationException(
        ErrorConstants.CODE_VALIDATION,
        ErrorConstants.MSG_VALIDATION,
        Map.of(TaskConstants.FIELD_TITLE, TaskConstants.MSG_TITLE_TOO_LONG));
  }

  public static ValidationException completedRequired() {
    return new ValidationException(
        ErrorConstants.CODE_VALIDATION,
        ErrorConstants.MSG_VALIDATION,
        Map.of(TaskConstants.FIELD_COMPLETED, TaskConstants.MSG_COMPLETED_REQUIRED));
  }

  public static ValidationException invalidFilter() {
    return new ValidationException(
        TaskConstants.CODE_INVALID_FILTER, TaskConstants.MSG_INVALID_FILTER);
  }

  public static ValidationException invalidId() {
    return new ValidationException(TaskConstants.CODE_INVALID_ID, TaskConstants.MSG_INVALID_ID);
  }

  public static NotFoundException notFound() {
    return new NotFoundException(TaskConstants.CODE_NOT_FOUND, TaskConstants.MSG_NOT_FOUND);
  }

  private TaskErrors() {}
}
