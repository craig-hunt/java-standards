package com.sagecrest.standards.domain.signups;

import com.sagecrest.standards.domain.errors.ValidationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Validates a submitted signup, reporting every field problem at once.
 *
 * <p>Returning on the first problem would make a form correct one field per round trip. Every check
 * runs, so a caller marks all of them together.
 *
 * <p>Each check is the value type's own constructor rather than a copy of its rule. The constructor
 * throws a failure carrying the one field it concerns, and this merges those maps, so a sentence
 * shown to a user is written in exactly one place. Building the types to find out whether they
 * build uses an exception as a result, which is worth saying out loud; the alternative was the same
 * rule expressed twice, once to report it and once to enforce it, and two expressions of one rule
 * drift.
 */
public final class SignupValidator {

  public static SignupValidation validate(SignupRequest request) {
    Map<String, String> problems = new HashMap<>();

    Optional<FullName> name = attempt(() -> new FullName(request.fullName()), problems);
    Optional<EmailAddress> email = attempt(() -> new EmailAddress(request.email()), problems);
    Optional<Plan> plan = attempt(() -> new Plan(request.plan()), problems);
    Optional<Seats> seats = attempt(() -> Seats.of(request.seats()), problems);
    Optional<Notes> notes = attempt(() -> new Notes(request.notes()), problems);

    if (!request.acceptTerms()) {
      problems.putAll(SignupErrors.termsRequired().fields());
    }

    if (!problems.isEmpty()) {
      return new SignupValidation(Optional.empty(), problems);
    }
    return new SignupValidation(
        Optional.of(
            new Signup(
                name.orElseThrow(),
                email.orElseThrow(),
                plan.orElseThrow(),
                seats.orElseThrow(),
                notes.orElseThrow())),
        problems);
  }

  private static <T> Optional<T> attempt(Supplier<T> build, Map<String, String> problems) {
    try {
      return Optional.of(build.get());
    } catch (ValidationException rejected) {
      problems.putAll(rejected.fields());
      return Optional.empty();
    }
  }

  private SignupValidator() {}
}
