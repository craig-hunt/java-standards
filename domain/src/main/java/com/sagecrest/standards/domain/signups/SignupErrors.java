package com.sagecrest.standards.domain.signups;

import com.sagecrest.standards.domain.errors.ErrorConstants;
import com.sagecrest.standards.domain.errors.ValidationException;
import java.util.Map;

/**
 * The failures the signup feature raises, each naming the field it belongs to.
 *
 * <p>Every one carries the shared envelope code and a single-entry fields map. That shape is what
 * lets {@link SignupValidator} collect a whole form's worth of problems by merging the maps of the
 * failures each value type threw, with no sentence written twice.
 */
public final class SignupErrors {

  public static ValidationException nameRequired() {
    return field(SignupConstants.FIELD_FULL_NAME, SignupConstants.MSG_NAME_REQUIRED);
  }

  public static ValidationException emailRequired() {
    return field(SignupConstants.FIELD_EMAIL, SignupConstants.MSG_EMAIL_REQUIRED);
  }

  public static ValidationException emailInvalid() {
    return field(SignupConstants.FIELD_EMAIL, SignupConstants.MSG_EMAIL_INVALID);
  }

  public static ValidationException planRequired() {
    return field(SignupConstants.FIELD_PLAN, SignupConstants.MSG_PLAN_REQUIRED);
  }

  public static ValidationException planUnknown() {
    return field(SignupConstants.FIELD_PLAN, SignupConstants.MSG_PLAN_UNKNOWN);
  }

  public static ValidationException seatsInvalid() {
    return field(SignupConstants.FIELD_SEATS, SignupConstants.MSG_SEATS_INVALID);
  }

  public static ValidationException notesTooLong() {
    return field(SignupConstants.FIELD_NOTES, SignupConstants.MSG_NOTES_TOO_LONG);
  }

  public static ValidationException termsRequired() {
    return field(SignupConstants.FIELD_ACCEPT_TERMS, SignupConstants.MSG_TERMS_REQUIRED);
  }

  public static ValidationException invalid(Map<String, String> problems) {
    return new ValidationException(
        ErrorConstants.CODE_VALIDATION, ErrorConstants.MSG_VALIDATION, problems);
  }

  public static ValidationException invalidId() {
    return new ValidationException(SignupConstants.CODE_INVALID_ID, SignupConstants.MSG_INVALID_ID);
  }

  private static ValidationException field(String name, String problem) {
    return new ValidationException(
        ErrorConstants.CODE_VALIDATION, ErrorConstants.MSG_VALIDATION, Map.of(name, problem));
  }

  private SignupErrors() {}
}
