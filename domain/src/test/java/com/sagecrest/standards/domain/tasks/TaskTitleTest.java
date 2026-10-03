package com.sagecrest.standards.domain.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.domain.errors.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TaskTitleTest {

  private static final String TITLE = "Write the decision record";
  private static final String PADDED = "  Write the decision record  ";
  private static final String LETTER = "a";
  private static final String EMOJI = "😀";
  private static final int ONE_TOO_MANY = TaskConstants.MAX_TITLE_LENGTH + 1;

  @Test
  void keepsAUsableTitle() {
    assertThat(new TaskTitle(TITLE).value()).isEqualTo(TITLE);
  }

  @Test
  @DisplayName("stores the trimmed value, so no caller holds an untrimmed title")
  void storesTheTrimmedValue() {
    assertThat(new TaskTitle(PADDED).value()).isEqualTo(TITLE);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t", "\n"})
  @DisplayName("treats absent, empty and whitespace-only titles the same way")
  void rejectsAnAbsentTitle(String raw) {
    assertThatThrownBy(() -> new TaskTitle(raw))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).fields())
        .isEqualTo(java.util.Map.of(TaskConstants.FIELD_TITLE, TaskConstants.MSG_TITLE_REQUIRED));
  }

  @Test
  @DisplayName("accepts a title of exactly the maximum length")
  void acceptsTheMaximumLength() {
    String atLimit = LETTER.repeat(TaskConstants.MAX_TITLE_LENGTH);
    assertThat(new TaskTitle(atLimit).value()).hasSize(TaskConstants.MAX_TITLE_LENGTH);
  }

  @Test
  @DisplayName("rejects a title one character past the maximum length")
  void rejectsOneCharacterPastTheMaximum() {
    String tooLong = LETTER.repeat(ONE_TOO_MANY);
    assertThatThrownBy(() -> new TaskTitle(tooLong))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).fields())
        .isEqualTo(java.util.Map.of(TaskConstants.FIELD_TITLE, TaskConstants.MSG_TITLE_TOO_LONG));
  }

  @Test
  @DisplayName("measures the limit in code points, so emoji are not counted twice")
  void measuresTheLimitInCodePoints() {
    String atLimit = EMOJI.repeat(TaskConstants.MAX_TITLE_LENGTH);
    assertThat(new TaskTitle(atLimit).value()).isEqualTo(atLimit);
  }

  @Test
  void printsItsValue() {
    assertThat(new TaskTitle(TITLE)).hasToString(TITLE);
  }
}
