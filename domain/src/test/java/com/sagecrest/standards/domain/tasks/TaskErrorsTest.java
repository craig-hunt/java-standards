package com.sagecrest.standards.domain.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.domain.errors.DomainException;
import com.sagecrest.standards.domain.errors.ErrorConstants;
import com.sagecrest.standards.domain.errors.NotFoundException;
import com.sagecrest.standards.domain.errors.ValidationException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts each failure directly rather than reaching it through the type that raises it.
 *
 * <p>Which code and which field sentence a failure carries is a decision a client depends on, so it
 * deserves a test that names it. Covering these only by provoking them through {@link TaskTitle}
 * and {@link TaskFilter} would leave the ones the outer layers raise untested in the module that
 * defines them.
 */
class TaskErrorsTest {

  @Test
  void namesTheTitleFieldWhenATitleIsMissing() {
    ValidationException failure = TaskErrors.titleRequired();

    assertThat(failure.code()).isEqualTo(ErrorConstants.CODE_VALIDATION);
    assertThat(failure).hasMessage(ErrorConstants.MSG_VALIDATION);
    assertThat(failure.fields())
        .containsExactly(Map.entry(TaskConstants.FIELD_TITLE, TaskConstants.MSG_TITLE_REQUIRED));
  }

  @Test
  void namesTheTitleFieldWhenATitleRunsLong() {
    ValidationException failure = TaskErrors.titleTooLong();

    assertThat(failure.code()).isEqualTo(ErrorConstants.CODE_VALIDATION);
    assertThat(failure.fields())
        .containsExactly(Map.entry(TaskConstants.FIELD_TITLE, TaskConstants.MSG_TITLE_TOO_LONG));
  }

  @Test
  @DisplayName("names the completed field when an update omits it")
  void namesTheCompletedFieldWhenAnUpdateOmitsIt() {
    ValidationException failure = TaskErrors.completedRequired();

    assertThat(failure.code()).isEqualTo(ErrorConstants.CODE_VALIDATION);
    assertThat(failure.fields())
        .containsExactly(
            Map.entry(TaskConstants.FIELD_COMPLETED, TaskConstants.MSG_COMPLETED_REQUIRED));
  }

  @Test
  @DisplayName("answers a bad filter and a bad id with their own codes, carrying no fields")
  void answersQueryProblemsWithoutFields() {
    ValidationException badFilter = TaskErrors.invalidFilter();
    ValidationException badId = TaskErrors.invalidId();

    assertThat(badFilter.code()).isEqualTo(TaskConstants.CODE_INVALID_FILTER);
    assertThat(badFilter).hasMessage(TaskConstants.MSG_INVALID_FILTER);
    assertThat(badFilter.fields()).isEmpty();
    assertThat(badId.code()).isEqualTo(TaskConstants.CODE_INVALID_ID);
    assertThat(badId).hasMessage(TaskConstants.MSG_INVALID_ID);
    assertThat(badId.fields()).isEmpty();
  }

  @Test
  @DisplayName("reports a missing task as not found, which the edge maps to its own status")
  void reportsAMissingTaskAsNotFound() {
    NotFoundException failure = TaskErrors.notFound();

    assertThat(failure.code()).isEqualTo(TaskConstants.CODE_NOT_FOUND);
    assertThat(failure).hasMessage(TaskConstants.MSG_NOT_FOUND);
    assertThat(failure.fields()).isEmpty();
    assertThat(failure).isInstanceOf(DomainException.class);
  }
}
