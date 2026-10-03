package com.sagecrest.standards.web.endpoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.application.ports.SignupStore;
import com.sagecrest.standards.application.signups.SignupService;
import com.sagecrest.standards.domain.errors.ValidationException;
import com.sagecrest.standards.domain.signups.Signup;
import com.sagecrest.standards.domain.signups.SignupConstants;
import com.sagecrest.standards.domain.signups.SignupId;
import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.routing.FakeExchange;
import com.sagecrest.standards.web.routing.Router;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignupEndpointsTest {

  private static final long ASSIGNED_ID = 12L;
  private static final String EXPECTED_SUMMARY = "Ada Lovelace on the Growth plan, 2 seat(s).";

  private static final String BODY_VALID =
      """
      {"fullName":"Ada Lovelace","email":"ada@example.com","plan":"Growth","seats":2,\
      "notes":"","acceptTerms":true}\
      """;
  private static final String BODY_EMPTY =
      """
      {"fullName":"","email":"","plan":"","seats":0,"notes":"","acceptTerms":false}\
      """;
  private static final String BODY_BAD_EMAIL =
      """
      {"fullName":"Ada Lovelace","email":"ada","plan":"Growth","seats":2,\
      "notes":"","acceptTerms":true}\
      """;

  private static final int EVERY_FIELD = 5;

  private final List<Signup> saved = new ArrayList<>();

  private final SignupStore store =
      signup -> {
        saved.add(signup);
        return new SignupId(ASSIGNED_ID);
      };

  private FakeExchange posted(String body) throws IOException {
    FakeExchange exchange =
        new FakeExchange(WebConstants.METHOD_POST, WebConstants.PATH_SIGNUPS, body);
    new Router(new SignupEndpoints(new SignupService(store)).routes()).handle(exchange);
    return exchange;
  }

  @Test
  @DisplayName("confirms with the identifier the store assigned and the signup's own summary")
  void confirmsWithTheAssignedIdentifier() throws IOException {
    FakeExchange exchange = posted(BODY_VALID);

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_CREATED);
    assertThat(exchange.responseText()).contains(String.valueOf(ASSIGNED_ID), EXPECTED_SUMMARY);
    assertThat(saved).hasSize(1);
  }

  @Test
  @DisplayName("reports every problem at once, keyed by the names the client sent")
  void reportsEveryProblemAtOnce() {
    assertThatThrownBy(() -> posted(BODY_EMPTY))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).fields())
        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
        .hasSize(EVERY_FIELD)
        .containsKeys(
            SignupConstants.FIELD_FULL_NAME,
            SignupConstants.FIELD_EMAIL,
            SignupConstants.FIELD_PLAN,
            SignupConstants.FIELD_SEATS,
            SignupConstants.FIELD_ACCEPT_TERMS);
  }

  @Test
  @DisplayName("nothing reaches the store when the form is rejected")
  void nothingReachesTheStoreWhenRejected() {
    assertThatThrownBy(() -> posted(BODY_BAD_EMAIL)).isInstanceOf(ValidationException.class);

    assertThat(saved).isEmpty();
  }
}
