package com.sagecrest.standards.application.signups;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.application.ports.SignupStore;
import com.sagecrest.standards.domain.signups.Plan;
import com.sagecrest.standards.domain.signups.Signup;
import com.sagecrest.standards.domain.signups.SignupConfirmation;
import com.sagecrest.standards.domain.signups.SignupConstants;
import com.sagecrest.standards.domain.signups.SignupId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignupServiceTest {

  private static final String NAME = "Ada Lovelace";
  private static final String EMAIL = "ada@example.com";
  private static final String NOTES = "Platform team first.";
  private static final int SEATS = 3;
  private static final long ASSIGNED_ID = 12L;
  private static final String EXPECTED_SUMMARY = "Ada Lovelace on the Growth plan, 3 seat(s).";

  private static final class CapturingSignupStore implements SignupStore {
    private final List<Signup> saved = new ArrayList<>();

    @Override
    public SignupId save(Signup signup) {
      saved.add(signup);
      return new SignupId(ASSIGNED_ID);
    }
  }

  private static Signup signup() {
    return new Signup(NAME, EMAIL, new Plan(SignupConstants.PLAN_GROWTH), SEATS, NOTES);
  }

  @Test
  @DisplayName("confirms with the identifier the store assigned, not one it invented")
  void confirmsWithTheIdentifierTheStoreAssigned() {
    CapturingSignupStore store = new CapturingSignupStore();

    SignupConfirmation confirmation = new SignupService(store).create(signup());

    assertThat(confirmation.id()).isEqualTo(new SignupId(ASSIGNED_ID));
    assertThat(confirmation.summary()).isEqualTo(EXPECTED_SUMMARY);
    assertThat(store.saved).containsExactly(signup());
  }
}
