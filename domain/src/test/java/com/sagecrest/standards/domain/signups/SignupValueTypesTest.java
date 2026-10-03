package com.sagecrest.standards.domain.signups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.domain.errors.ValidationException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Asserts that each field of a signup refuses to exist in a state the form would reject.
 *
 * <p>These replace a test that checked a {@code Plan} could report itself absent. Reporting is not
 * enforcing: the old shape let a caller build a signup around an absent plan and ask later.
 */
class SignupValueTypesTest {

  private static final String NAME = "Ada Lovelace";
  private static final String PADDED_NAME = "  Ada Lovelace  ";
  private static final String EMAIL = "ada@example.com";
  private static final String PADDED_EMAIL = "  ada@example.com  ";
  private static final String UNKNOWN_PLAN = "Platinum";
  private static final String LETTER = "a";
  private static final String NOTE = "Platform team first.";
  private static final String PADDING = "  ";
  private static final int NO_SEATS = 0;
  private static final int NEGATIVE_SEATS = -4;
  private static final int MANY_SEATS = 25;
  private static final int ONE_TOO_LONG = SignupConstants.MAX_NOTES_LENGTH + 1;

  private static Map<String, String> fieldsOf(ThrowingCall call) {
    try {
      call.run();
      throw new AssertionError(NAME);
    } catch (ValidationException rejected) {
      return rejected.fields();
    }
  }

  @FunctionalInterface
  private interface ThrowingCall {
    void run();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  @DisplayName("a name cannot be absent, empty or whitespace")
  void aNameCannotBeAbsent(String raw) {
    assertThat(fieldsOf(() -> new FullName(raw)))
        .containsExactly(
            Map.entry(SignupConstants.FIELD_FULL_NAME, SignupConstants.MSG_NAME_REQUIRED));
  }

  @Test
  void aNameStoresItselfTrimmed() {
    assertThat(new FullName(PADDED_NAME).value()).isEqualTo(NAME);
    assertThat(new FullName(NAME)).hasToString(NAME);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("an absent email reads as absent, not as malformed")
  void anAbsentEmailReadsAsAbsent(String raw) {
    assertThat(fieldsOf(() -> new EmailAddress(raw)))
        .containsExactly(
            Map.entry(SignupConstants.FIELD_EMAIL, SignupConstants.MSG_EMAIL_REQUIRED));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ada", "ada@", "@example.com", "ada@example", "ada example.com"})
  void aMalformedEmailReadsAsMalformed(String raw) {
    assertThat(fieldsOf(() -> new EmailAddress(raw)))
        .containsExactly(Map.entry(SignupConstants.FIELD_EMAIL, SignupConstants.MSG_EMAIL_INVALID));
  }

  @Test
  void anEmailStoresItselfTrimmed() {
    assertThat(new EmailAddress(PADDED_EMAIL).value()).isEqualTo(EMAIL);
    assertThat(new EmailAddress(EMAIL)).hasToString(EMAIL);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName("an absent plan reads as absent, not as unknown")
  void anAbsentPlanReadsAsAbsent(String raw) {
    assertThat(fieldsOf(() -> new Plan(raw)))
        .containsExactly(Map.entry(SignupConstants.FIELD_PLAN, SignupConstants.MSG_PLAN_REQUIRED));
  }

  @Test
  void anUnknownPlanNamesThePlansOnOffer() {
    assertThat(fieldsOf(() -> new Plan(UNKNOWN_PLAN)))
        .containsExactly(Map.entry(SignupConstants.FIELD_PLAN, SignupConstants.MSG_PLAN_UNKNOWN));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        SignupConstants.PLAN_STARTER,
        SignupConstants.PLAN_GROWTH,
        SignupConstants.PLAN_ENTERPRISE
      })
  void everyPlanOnOfferIsAccepted(String raw) {
    assertThat(new Plan(raw).value()).isEqualTo(raw);
    assertThat(new Plan(raw)).hasToString(raw);
  }

  @Test
  @DisplayName("an omitted seat count defaults, and an explicit zero does not")
  void anOmittedSeatCountDefaults() {
    assertThat(Seats.of(null).value()).isEqualTo(SignupConstants.DEFAULT_SEATS);
    assertThat(Seats.of(MANY_SEATS).value()).isEqualTo(MANY_SEATS);
    assertThat(fieldsOf(() -> Seats.of(NO_SEATS)))
        .containsExactly(Map.entry(SignupConstants.FIELD_SEATS, SignupConstants.MSG_SEATS_INVALID));
    assertThat(fieldsOf(() -> Seats.of(NEGATIVE_SEATS)))
        .containsExactly(Map.entry(SignupConstants.FIELD_SEATS, SignupConstants.MSG_SEATS_INVALID));
  }

  @Test
  @DisplayName("the smallest seat count on offer is accepted")
  void theSmallestSeatCountIsAccepted() {
    Seats smallest = new Seats(SignupConstants.MIN_SEATS);

    assertThat(smallest.value()).isEqualTo(SignupConstants.MIN_SEATS);
    assertThat(smallest).hasToString(Integer.toString(SignupConstants.MIN_SEATS));
  }

  @Test
  @DisplayName("notes are optional, trimmed, and capped at the limit")
  void notesAreOptionalAndCapped() {
    assertThat(new Notes(null).value()).isEmpty();
    assertThat(new Notes(PADDING + NOTE + PADDING).value()).isEqualTo(NOTE);
    assertThat(new Notes(LETTER.repeat(SignupConstants.MAX_NOTES_LENGTH)).value())
        .hasSize(SignupConstants.MAX_NOTES_LENGTH);
    assertThat(new Notes(NOTE)).hasToString(NOTE);
  }

  @Test
  void notesOneCharacterPastTheLimitAreRejected() {
    assertThat(fieldsOf(() -> new Notes(LETTER.repeat(ONE_TOO_LONG))))
        .containsExactly(
            Map.entry(SignupConstants.FIELD_NOTES, SignupConstants.MSG_NOTES_TOO_LONG));
  }

  @Test
  @DisplayName("a signup cannot be assembled out of values the form would reject")
  void aSignupCannotBeAssembledOutOfRejectedValues() {
    assertThatThrownBy(() -> new Signup(new FullName(null), null, null, null, null))
        .isInstanceOf(ValidationException.class);
  }
}
