package com.sagecrest.standards.domain.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.domain.errors.ValidationException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TaskIdTest {

  private static final long FIRST_VALID = 1L;
  private static final long BELOW_FIRST_VALID = 0L;
  private static final long NEGATIVE = -1L;
  private static final long LATER = 42L;

  private static final String FIRST_VALID_TEXT = "1";
  private static final String BELOW_FIRST_VALID_TEXT = "0";
  private static final String LATER_TEXT = "42";

  @Test
  @DisplayName("accepts the first valid identifier")
  void acceptsFirstValidIdentifier() {
    assertThat(new TaskId(FIRST_VALID).value()).isEqualTo(FIRST_VALID);
  }

  @Test
  @DisplayName("rejects the value one below the first valid identifier")
  void rejectsValueBelowFirstValid() {
    assertThatThrownBy(() -> new TaskId(BELOW_FIRST_VALID))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).code())
        .isEqualTo(TaskConstants.CODE_INVALID_ID);
  }

  @Test
  void rejectsANegativeIdentifier() {
    assertThatThrownBy(() -> new TaskId(NEGATIVE)).isInstanceOf(ValidationException.class);
  }

  @Test
  void parsesADigitString() {
    assertThat(TaskId.parse(LATER_TEXT)).contains(new TaskId(LATER));
  }

  @Test
  @DisplayName("parses the first valid identifier but not the one below it")
  void parsesAtTheBoundary() {
    assertThat(TaskId.parse(FIRST_VALID_TEXT)).contains(new TaskId(FIRST_VALID));
    assertThat(TaskId.parse(BELOW_FIRST_VALID_TEXT)).isEmpty();
  }

  @ParameterizedTest
  @DisplayName("refuses every spelling the Go and C# siblings also refuse")
  @ValueSource(
      strings = {" 1", "1 ", "+1", "-1", "1.0", "", "abc", "1a", "١", "9223372036854775808"})
  void refusesEverythingThatIsNotPlainAsciiDigits(String raw) {
    assertThat(TaskId.parse(raw)).isEmpty();
  }

  @ParameterizedTest
  @DisplayName("accepts every ASCII digit, including the two at the ends of the range")
  @ValueSource(strings = {"10", "19", "90", "109", "1234567890"})
  void acceptsEveryAsciiDigit(String raw) {
    assertThat(TaskId.parse(raw)).contains(new TaskId(Long.parseLong(raw)));
  }

  @Test
  void refusesAnAbsentRouteValue() {
    assertThat(TaskId.parse(null)).isEqualTo(Optional.empty());
  }

  @Test
  void printsTheBareNumber() {
    assertThat(new TaskId(LATER)).hasToString(LATER_TEXT);
  }
}
