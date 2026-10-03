package com.sagecrest.standards.domain.signups;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.domain.errors.ErrorConstants;
import com.sagecrest.standards.domain.errors.ValidationException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignupErrorsTest {

  private static final String PROBLEM = "Enter something.";

  @Test
  @DisplayName("passes the validator's problems through under the shared envelope code")
  void passesTheValidatorProblemsThrough() {
    Map<String, String> problems = Map.of(SignupConstants.FIELD_EMAIL, PROBLEM);

    ValidationException failure = SignupErrors.invalid(problems);

    assertThat(failure.code()).isEqualTo(ErrorConstants.CODE_VALIDATION);
    assertThat(failure).hasMessage(ErrorConstants.MSG_VALIDATION);
    assertThat(failure.fields()).isEqualTo(problems);
  }

  @Test
  void answersABadSignupIdWithItsOwnCode() {
    ValidationException failure = SignupErrors.invalidId();

    assertThat(failure.code()).isEqualTo(SignupConstants.CODE_INVALID_ID);
    assertThat(failure).hasMessage(SignupConstants.MSG_INVALID_ID);
    assertThat(failure.fields()).isEmpty();
  }
}
