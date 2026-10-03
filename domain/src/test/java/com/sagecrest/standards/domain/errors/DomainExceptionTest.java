package com.sagecrest.standards.domain.errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainExceptionTest {

  private static final String CODE = "a_code";
  private static final String MESSAGE = "a message";
  private static final String FIELD = "aField";
  private static final String PROBLEM = "a problem";
  private static final String OTHER = "other";

  @Test
  void carriesItsCodeAndMessage() {
    DomainException failure = new ValidationException(CODE, MESSAGE);

    assertThat(failure.code()).isEqualTo(CODE);
    assertThat(failure).hasMessage(MESSAGE);
  }

  @Test
  @DisplayName("reports no fields when none apply")
  void reportsNoFieldsWhenNoneApply() {
    assertThat(new ValidationException(CODE, MESSAGE).fields()).isEmpty();
    assertThat(new NotFoundException(CODE, MESSAGE).fields()).isEmpty();
  }

  @Test
  void carriesThePerFieldProblems() {
    DomainException failure = new ValidationException(CODE, MESSAGE, Map.of(FIELD, PROBLEM));

    assertThat(failure.fields()).containsExactly(Map.entry(FIELD, PROBLEM));
  }

  @Test
  @DisplayName("copies the problems, so the caller cannot change them afterwards")
  void copiesTheProblems() {
    Map<String, String> mutable = new HashMap<>();
    mutable.put(FIELD, PROBLEM);
    DomainException failure = new ValidationException(CODE, MESSAGE, mutable);

    mutable.put(OTHER, PROBLEM);

    assertThat(failure.fields()).containsExactly(Map.entry(FIELD, PROBLEM));
    assertThatThrownBy(() -> failure.fields().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("stays unchecked, so nothing between the domain and the edge declares it")
  void staysUnchecked() {
    assertThat(RuntimeException.class).isAssignableFrom(DomainException.class);
  }
}
