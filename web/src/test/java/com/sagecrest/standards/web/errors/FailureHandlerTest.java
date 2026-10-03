package com.sagecrest.standards.web.errors;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.domain.errors.ErrorConstants;
import com.sagecrest.standards.domain.errors.NotFoundException;
import com.sagecrest.standards.domain.errors.ValidationException;
import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.routing.FakeExchange;
import com.sagecrest.standards.web.routing.Responses;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FailureHandlerTest {

  private static final String CODE = "a_code";
  private static final String DETAIL = "a detail";
  private static final String FIELD = "title";
  private static final String PROBLEM = "Enter a title.";
  private static final String INTERNALS = "relation tasks does not exist";
  private static final String PASSED_THROUGH = "handled";
  private static final String FIELDS_MEMBER = "fields";

  private static FakeExchange exchange() {
    return new FakeExchange(WebConstants.METHOD_GET, WebConstants.PATH_TASKS);
  }

  private static FakeExchange answering(HttpHandler failing) throws IOException {
    FakeExchange exchange = exchange();
    new FailureHandler(failing).handle(exchange);
    return exchange;
  }

  @Test
  @DisplayName("a validation failure answers 422 with the field the message belongs to")
  void aValidationFailureAnswers422() throws IOException {
    FakeExchange exchange =
        answering(
            ignored -> {
              throw new ValidationException(CODE, DETAIL, Map.of(FIELD, PROBLEM));
            });

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_UNPROCESSABLE);
    assertThat(exchange.responseHeader(WebConstants.HEADER_CONTENT_TYPE))
        .isEqualTo(WebConstants.CONTENT_TYPE_PROBLEM);
    assertThat(exchange.responseText())
        .contains(CODE, DETAIL, FIELD, PROBLEM, WebConstants.TITLE_UNPROCESSABLE);
  }

  @Test
  @DisplayName("a not-found failure answers 404, and carries no fields member")
  void aNotFoundFailureAnswers404() throws IOException {
    FakeExchange exchange =
        answering(
            ignored -> {
              throw new NotFoundException(CODE, DETAIL);
            });

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_NOT_FOUND);
    assertThat(exchange.responseText()).contains(CODE, DETAIL, WebConstants.TITLE_NOT_FOUND);
    assertThat(exchange.responseText()).doesNotContain(FIELDS_MEMBER);
  }

  @Test
  @DisplayName("a malformed body answers 400 with the shared invalid-body code")
  void aMalformedBodyAnswers400() throws IOException {
    FakeExchange exchange =
        answering(
            ignored -> {
              throw new InvalidRequestBodyException();
            });

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_BAD_REQUEST);
    assertThat(exchange.responseText())
        .contains(ErrorConstants.CODE_INVALID_BODY, WebConstants.TITLE_BAD_REQUEST);
  }

  @Test
  @DisplayName("an unexpected failure answers 500 and tells the client nothing about the internals")
  void anUnexpectedFailureAnswers500WithoutInternals() throws IOException {
    FakeExchange exchange =
        answering(
            ignored -> {
              throw new IllegalStateException(INTERNALS);
            });

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_INTERNAL);
    assertThat(exchange.responseText())
        .contains(ErrorConstants.CODE_INTERNAL, ErrorConstants.MSG_INTERNAL);
    assertThat(exchange.responseText())
        .as("a message naming a table tells an attacker more than it tells the caller")
        .doesNotContain(INTERNALS);
  }

  @Test
  @DisplayName("a failure raised while reading answers 500 rather than escaping")
  void anIoFailureAnswers500() throws IOException {
    FakeExchange exchange =
        answering(
            ignored -> {
              throw new IOException(INTERNALS);
            });

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_INTERNAL);
  }

  @Test
  @DisplayName("a handler that succeeded is left alone")
  void aSucceedingHandlerIsLeftAlone() throws IOException {
    FakeExchange exchange = exchange();

    new FailureHandler(
            sent -> Responses.json(sent, WebConstants.STATUS_OK, Map.of(FIELD, PASSED_THROUGH)))
        .handle(exchange);

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_OK);
    assertThat(exchange.responseText()).contains(PASSED_THROUGH);
  }

  @Test
  @DisplayName("a failure after the response began is logged, not written over")
  void aFailureAfterTheResponseBeganIsNotWrittenOver() throws IOException {
    FakeExchange exchange = exchange();

    new FailureHandler(
            started -> {
              Responses.json(started, WebConstants.STATUS_OK, Map.of(FIELD, PASSED_THROUGH));
              throw new IllegalStateException(INTERNALS);
            })
        .handle(exchange);

    assertThat(exchange.status())
        .as("the status a client already received cannot be taken back")
        .isEqualTo(WebConstants.STATUS_OK);
  }
}
