package com.sagecrest.standards.web.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.routing.FakeExchange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BearerTokenHandlerTest {

  private static final String TOKEN = "a-token";
  private static final String PASSED_THROUGH = "handled";

  private final List<String> reached = new ArrayList<>();

  private static FakeExchange request() {
    return new FakeExchange(WebConstants.METHOD_GET, WebConstants.PATH_TASKS);
  }

  private FakeExchange handled(FakeExchange exchange) throws IOException {
    new BearerTokenHandler(ignored -> reached.add(PASSED_THROUGH), TOKEN).handle(exchange);
    return exchange;
  }

  @Test
  void admitsTheCorrectToken() throws IOException {
    FakeExchange exchange =
        handled(
            request()
                .withRequestHeader(
                    WebConstants.HEADER_AUTHORIZATION, WebConstants.SCHEME_BEARER_PREFIX + TOKEN));

    assertThat(reached).containsExactly(PASSED_THROUGH);
    assertThat(exchange.status()).isEqualTo(-1);
  }

  @Test
  @DisplayName("refuses a request with no authorization at all, and says how to present one")
  void refusesARequestWithNoAuthorization() throws IOException {
    FakeExchange exchange = handled(request());

    assertThat(reached).isEmpty();
    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_UNAUTHORIZED);
    assertThat(exchange.responseHeader(WebConstants.HEADER_WWW_AUTHENTICATE))
        .isEqualTo(WebConstants.CHALLENGE_BEARER);
    assertThat(exchange.responseText()).contains(WebConstants.CODE_UNAUTHORIZED);
  }

  @ParameterizedTest
  @DisplayName(
      "refuses a token that is wrong, truncated, extended, or presented without the scheme")
  @ValueSource(
      strings = {
        "Bearer not-the-token",
        "Bearer a-toke",
        "Bearer a-tokenn",
        "Bearer ",
        "a-token",
        "Basic a-token",
        "bearer a-token"
      })
  void refusesEveryWrongPresentation(String header) throws IOException {
    FakeExchange exchange =
        handled(request().withRequestHeader(WebConstants.HEADER_AUTHORIZATION, header));

    assertThat(reached).isEmpty();
    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_UNAUTHORIZED);
  }

  @Test
  @DisplayName("nothing downstream runs for a rejected request")
  void nothingDownstreamRunsForARejectedRequest() throws IOException {
    handled(request().withRequestHeader(WebConstants.HEADER_AUTHORIZATION, TOKEN));

    assertThat(reached).as("a check that ran after the handler would be no check at all").isEmpty();
  }
}
