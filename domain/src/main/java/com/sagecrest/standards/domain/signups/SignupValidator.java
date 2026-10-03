package com.sagecrest.standards.domain.signups;

import com.sagecrest.standards.domain.text.TextLength;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validates a submitted signup, reporting every field problem at once.
 *
 * <p>Returning on the first problem would make a form correct one field per round trip. Every check
 * runs, so a caller marks all of them together.
 */
public final class SignupValidator {

  private static final String ABSENT = "";

  private static final Pattern EMAIL = Pattern.compile(SignupConstants.EMAIL_PATTERN);

  public static SignupValidation validate(SignupRequest request) {
    Map<String, String> problems = new HashMap<>();

    String name = trimmed(request.fullName());
    if (name.isEmpty()) {
      problems.put(SignupConstants.FIELD_FULL_NAME, SignupConstants.MSG_NAME_REQUIRED);
    }

    String email = trimmed(request.email());
    if (email.isEmpty()) {
      problems.put(SignupConstants.FIELD_EMAIL, SignupConstants.MSG_EMAIL_REQUIRED);
    } else if (!EMAIL.matcher(email).matches()) {
      problems.put(SignupConstants.FIELD_EMAIL, SignupConstants.MSG_EMAIL_INVALID);
    }

    Plan plan = new Plan(request.plan());
    if (plan.isAbsent()) {
      problems.put(SignupConstants.FIELD_PLAN, SignupConstants.MSG_PLAN_REQUIRED);
    } else if (!plan.known()) {
      problems.put(SignupConstants.FIELD_PLAN, SignupConstants.MSG_PLAN_UNKNOWN);
    }

    int seats = SignupConstants.DEFAULT_SEATS;
    if (request.seats() != null) {
      seats = request.seats();
      if (seats < SignupConstants.MIN_SEATS) {
        problems.put(SignupConstants.FIELD_SEATS, SignupConstants.MSG_SEATS_INVALID);
      }
    }

    String notes = trimmed(request.notes());
    if (TextLength.countCodePoints(notes) > SignupConstants.MAX_NOTES_LENGTH) {
      problems.put(SignupConstants.FIELD_NOTES, SignupConstants.MSG_NOTES_TOO_LONG);
    }

    if (!request.acceptTerms()) {
      problems.put(SignupConstants.FIELD_ACCEPT_TERMS, SignupConstants.MSG_TERMS_REQUIRED);
    }

    Optional<Signup> validated =
        problems.isEmpty()
            ? Optional.of(new Signup(name, email, plan, seats, notes))
            : Optional.empty();
    return new SignupValidation(validated, problems);
  }

  private static String trimmed(String raw) {
    return Objects.requireNonNullElse(raw, ABSENT).trim();
  }

  private SignupValidator() {}
}
