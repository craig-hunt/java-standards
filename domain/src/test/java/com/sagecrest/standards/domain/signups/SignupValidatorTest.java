package com.sagecrest.standards.domain.signups;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SignupValidatorTest {

  private static final String NAME = "Ada Lovelace";
  private static final String PADDED_NAME = "  Ada Lovelace  ";
  private static final String EMAIL = "ada@example.com";
  private static final String NOTES = "Rolling out to the platform team first.";
  private static final String LETTER = "a";
  private static final int ONE_NOTE_TOO_LONG = SignupConstants.MAX_NOTES_LENGTH + 1;
  private static final int NO_SEATS = 0;
  private static final int MANY_SEATS = 25;
  private static final String UNKNOWN_PLAN = "Platinum";
  private static final int EVERY_FIELD = 6;

  private static SignupRequest valid() {
    return new SignupRequest(NAME, EMAIL, SignupConstants.PLAN_GROWTH, MANY_SEATS, NOTES, true);
  }

  @Test
  void acceptsACompleteSignup() {
    SignupValidation outcome = SignupValidator.validate(valid());

    assertThat(outcome.valid()).isTrue();
    assertThat(outcome.problems()).isEmpty();
    assertThat(outcome.value()).isPresent();
    assertThat(outcome.value().orElseThrow().seats().value()).isEqualTo(MANY_SEATS);
  }

  @Test
  @DisplayName("trims the name and notes it stores")
  void trimsWhatItStores() {
    SignupRequest padded =
        new SignupRequest(
            PADDED_NAME, EMAIL, SignupConstants.PLAN_STARTER, MANY_SEATS, NOTES, true);

    assertThat(SignupValidator.validate(padded).value().orElseThrow().fullName().value())
        .isEqualTo(NAME);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void requiresAName(String raw) {
    SignupRequest request =
        new SignupRequest(raw, EMAIL, SignupConstants.PLAN_GROWTH, MANY_SEATS, NOTES, true);

    assertThat(SignupValidator.validate(request).problems())
        .containsEntry(SignupConstants.FIELD_FULL_NAME, SignupConstants.MSG_NAME_REQUIRED);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void requiresAnEmail(String raw) {
    SignupRequest request =
        new SignupRequest(NAME, raw, SignupConstants.PLAN_GROWTH, MANY_SEATS, NOTES, true);

    assertThat(SignupValidator.validate(request).problems())
        .containsEntry(SignupConstants.FIELD_EMAIL, SignupConstants.MSG_EMAIL_REQUIRED);
  }

  @ParameterizedTest
  @DisplayName("tells an absent email apart from a malformed one")
  @ValueSource(strings = {"ada", "ada@", "@example.com", "ada@example", "ada example.com"})
  void rejectsAMalformedEmail(String raw) {
    SignupRequest request =
        new SignupRequest(NAME, raw, SignupConstants.PLAN_GROWTH, MANY_SEATS, NOTES, true);

    assertThat(SignupValidator.validate(request).problems())
        .containsEntry(SignupConstants.FIELD_EMAIL, SignupConstants.MSG_EMAIL_INVALID);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("asks for a plan when none arrived")
  void requiresAPlan(String raw) {
    SignupRequest request = new SignupRequest(NAME, EMAIL, raw, MANY_SEATS, NOTES, true);

    assertThat(SignupValidator.validate(request).problems())
        .containsEntry(SignupConstants.FIELD_PLAN, SignupConstants.MSG_PLAN_REQUIRED);
  }

  @Test
  @DisplayName("names the plans on offer when the submitted one is not among them")
  void rejectsAnUnknownPlan() {
    SignupRequest request = new SignupRequest(NAME, EMAIL, UNKNOWN_PLAN, MANY_SEATS, NOTES, true);

    assertThat(SignupValidator.validate(request).problems())
        .containsEntry(SignupConstants.FIELD_PLAN, SignupConstants.MSG_PLAN_UNKNOWN);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        SignupConstants.PLAN_STARTER,
        SignupConstants.PLAN_GROWTH,
        SignupConstants.PLAN_ENTERPRISE
      })
  void acceptsEveryPlanOnOffer(String plan) {
    SignupRequest request = new SignupRequest(NAME, EMAIL, plan, MANY_SEATS, NOTES, true);

    assertThat(SignupValidator.validate(request).valid()).isTrue();
  }

  @Test
  @DisplayName("defaults an omitted seat count rather than failing it")
  void defaultsAnOmittedSeatCount() {
    SignupRequest request =
        new SignupRequest(NAME, EMAIL, SignupConstants.PLAN_GROWTH, null, NOTES, true);
    SignupValidation outcome = SignupValidator.validate(request);

    assertThat(outcome.valid()).isTrue();
    assertThat(outcome.value().orElseThrow().seats().value())
        .isEqualTo(SignupConstants.DEFAULT_SEATS);
  }

  @Test
  @DisplayName("fails an explicit zero, which is what distinguishes it from an omission")
  void failsAnExplicitZeroSeatCount() {
    SignupRequest request =
        new SignupRequest(NAME, EMAIL, SignupConstants.PLAN_GROWTH, NO_SEATS, NOTES, true);

    assertThat(SignupValidator.validate(request).problems())
        .containsEntry(SignupConstants.FIELD_SEATS, SignupConstants.MSG_SEATS_INVALID);
  }

  @Test
  @DisplayName("accepts the smallest seat count on offer")
  void acceptsTheSmallestSeatCount() {
    SignupRequest request =
        new SignupRequest(
            NAME, EMAIL, SignupConstants.PLAN_GROWTH, SignupConstants.MIN_SEATS, NOTES, true);

    assertThat(SignupValidator.validate(request).valid()).isTrue();
  }

  @Test
  void acceptsNotesOfExactlyTheMaximumLength() {
    SignupRequest request =
        new SignupRequest(
            NAME,
            EMAIL,
            SignupConstants.PLAN_GROWTH,
            MANY_SEATS,
            LETTER.repeat(SignupConstants.MAX_NOTES_LENGTH),
            true);

    assertThat(SignupValidator.validate(request).valid()).isTrue();
  }

  @Test
  void rejectsNotesOneCharacterPastTheMaximum() {
    SignupRequest request =
        new SignupRequest(
            NAME,
            EMAIL,
            SignupConstants.PLAN_GROWTH,
            MANY_SEATS,
            LETTER.repeat(ONE_NOTE_TOO_LONG),
            true);

    assertThat(SignupValidator.validate(request).problems())
        .containsEntry(SignupConstants.FIELD_NOTES, SignupConstants.MSG_NOTES_TOO_LONG);
  }

  @Test
  void acceptsAnAbsentNote() {
    SignupRequest request =
        new SignupRequest(NAME, EMAIL, SignupConstants.PLAN_GROWTH, MANY_SEATS, null, true);

    assertThat(SignupValidator.validate(request).valid()).isTrue();
  }

  @Test
  void requiresTheTermsToBeAccepted() {
    SignupRequest request =
        new SignupRequest(NAME, EMAIL, SignupConstants.PLAN_GROWTH, MANY_SEATS, NOTES, false);

    assertThat(SignupValidator.validate(request).problems())
        .containsEntry(SignupConstants.FIELD_ACCEPT_TERMS, SignupConstants.MSG_TERMS_REQUIRED);
  }

  @Test
  @DisplayName("reports every problem at once, so a form corrects them in one pass")
  void reportsEveryProblemAtOnce() {
    SignupRequest empty =
        new SignupRequest(null, null, null, NO_SEATS, LETTER.repeat(ONE_NOTE_TOO_LONG), false);
    Map<String, String> problems = SignupValidator.validate(empty).problems();

    assertThat(problems).hasSize(EVERY_FIELD);
    assertThat(problems)
        .containsKeys(
            SignupConstants.FIELD_FULL_NAME,
            SignupConstants.FIELD_EMAIL,
            SignupConstants.FIELD_PLAN,
            SignupConstants.FIELD_SEATS,
            SignupConstants.FIELD_NOTES,
            SignupConstants.FIELD_ACCEPT_TERMS);
  }

  @Test
  @DisplayName("withholds the signup whenever any problem stands")
  void withholdsTheSignupWhenAnyProblemStands() {
    SignupRequest request =
        new SignupRequest(NAME, EMAIL, SignupConstants.PLAN_GROWTH, MANY_SEATS, NOTES, false);
    SignupValidation outcome = SignupValidator.validate(request);

    assertThat(outcome.valid()).isFalse();
    assertThat(outcome.value()).isEmpty();
  }
}
