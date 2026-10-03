package com.sagecrest.standards.web.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.errors.Problem;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResponsesTest {

  private record Body(String name) {}

  private static final String NAME = "Ada";
  private static final String EXPECTED_JSON = "{\"name\":\"Ada\"}";
  private static final String CODE = "a_code";
  private static final String DETAIL = "a detail";
  private static final int NO_BODY = -1;
  private static final int NOT_YET_SENT = -1;
  private static final String FIELDS_MEMBER = "fields";

  private static FakeExchange exchange() {
    return new FakeExchange(WebConstants.METHOD_GET, WebConstants.PATH_TASKS);
  }

  @Test
  @DisplayName("a JSON response carries its media type and its exact length")
  void aJsonResponseCarriesItsTypeAndLength() throws IOException {
    FakeExchange exchange = exchange();

    Responses.json(exchange, WebConstants.STATUS_OK, new Body(NAME));

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_OK);
    assertThat(exchange.responseHeader(WebConstants.HEADER_CONTENT_TYPE))
        .isEqualTo(WebConstants.CONTENT_TYPE_JSON);
    assertThat(exchange.responseText()).isEqualTo(EXPECTED_JSON);
    assertThat(exchange.declaredLength())
        .as("a declared length of zero would mean a chunked response")
        .isEqualTo(EXPECTED_JSON.length());
  }

  @Test
  @DisplayName("an empty response declares no body rather than a body of length zero")
  void anEmptyResponseDeclaresNoBody() throws IOException {
    FakeExchange exchange = exchange();

    Responses.empty(exchange, WebConstants.STATUS_NO_CONTENT);

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_NO_CONTENT);
    assertThat(exchange.declaredLength()).isEqualTo(NO_BODY);
    assertThat(exchange.responseText()).isEmpty();
  }

  @Test
  @DisplayName("a problem carries the problem media type, not the JSON one")
  void aProblemCarriesTheProblemMediaType() throws IOException {
    FakeExchange exchange = exchange();

    Responses.problem(
        exchange,
        Problem.of(WebConstants.STATUS_NOT_FOUND, WebConstants.TITLE_NOT_FOUND, CODE, DETAIL));

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_NOT_FOUND);
    assertThat(exchange.responseHeader(WebConstants.HEADER_CONTENT_TYPE))
        .isEqualTo(WebConstants.CONTENT_TYPE_PROBLEM);
    assertThat(exchange.responseText()).contains(CODE, DETAIL);
  }

  @Test
  @DisplayName("a problem omits the fields member when there are none to render")
  void aProblemOmitsAnEmptyFieldsMember() throws IOException {
    FakeExchange exchange = exchange();

    Responses.problem(
        exchange,
        Problem.of(
            WebConstants.STATUS_NOT_FOUND,
            WebConstants.TITLE_NOT_FOUND,
            CODE,
            DETAIL,
            java.util.Map.of()));

    assertThat(exchange.responseText()).doesNotContain(FIELDS_MEMBER);
  }

  @Test
  @DisplayName("a response that has begun is reported as begun, so nothing tries to replace it")
  void aStartedResponseIsReportedAsStarted() throws IOException {
    FakeExchange exchange = exchange();

    assertThat(exchange.getResponseCode()).isEqualTo(NOT_YET_SENT);
    assertThat(Responses.alreadyStarted(exchange)).isFalse();

    Responses.empty(exchange, WebConstants.STATUS_NO_CONTENT);

    assertThat(Responses.alreadyStarted(exchange)).isTrue();
  }
}
