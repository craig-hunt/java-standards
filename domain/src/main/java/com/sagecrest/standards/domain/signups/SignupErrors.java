package com.sagecrest.standards.domain.signups;

import com.sagecrest.standards.domain.errors.ErrorConstants;
import com.sagecrest.standards.domain.errors.ValidationException;
import java.util.Map;

/** The failures the signup feature raises. */
public final class SignupErrors {

  public static ValidationException invalid(Map<String, String> problems) {
    return new ValidationException(
        ErrorConstants.CODE_VALIDATION, ErrorConstants.MSG_VALIDATION, problems);
  }

  public static ValidationException invalidId() {
    return new ValidationException(SignupConstants.CODE_INVALID_ID, SignupConstants.MSG_INVALID_ID);
  }

  private SignupErrors() {}
}
