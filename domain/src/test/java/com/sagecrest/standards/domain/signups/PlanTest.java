package com.sagecrest.standards.domain.signups;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PlanTest {

  private static final String UNKNOWN = "Platinum";

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("reads an absent plan as absent rather than as unknown")
  void readsAnAbsentPlanAsAbsent(String raw) {
    Plan plan = new Plan(raw);

    assertThat(plan.isAbsent()).isTrue();
    assertThat(plan.known()).isFalse();
  }

  @Test
  @DisplayName("tells an unknown plan apart from an absent one")
  void tellsUnknownApartFromAbsent() {
    Plan plan = new Plan(UNKNOWN);

    assertThat(plan.isAbsent()).isFalse();
    assertThat(plan.known()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        SignupConstants.PLAN_STARTER,
        SignupConstants.PLAN_GROWTH,
        SignupConstants.PLAN_ENTERPRISE
      })
  void recognizesEveryPlanOnOffer(String raw) {
    Plan plan = new Plan(raw);

    assertThat(plan.known()).isTrue();
    assertThat(plan.isAbsent()).isFalse();
    assertThat(plan).hasToString(raw);
  }
}
