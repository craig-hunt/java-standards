package com.sagecrest.standards.web.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.web.WebConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;

class RequestIdHandlerTest {

  private static final String SAFE = "abc-123_4.5";
  private static final String TOO_LONG = "x".repeat(WebConstants.MAX_REQUEST_ID_LENGTH + 1);
  private static final String AT_THE_LIMIT = "y".repeat(WebConstants.MAX_REQUEST_ID_LENGTH);
  private static final String PASSED_THROUGH = "handled";

  private final List<String> seenInContext = new ArrayList<>();

  private FakeExchange handled(FakeExchange exchange) throws IOException {
    new RequestIdHandler(ignored -> seenInContext.add(MDC.get(WebConstants.HEADER_REQUEST_ID)))
        .handle(exchange);
    return exchange;
  }

  private static FakeExchange request() {
    return new FakeExchange(WebConstants.METHOD_GET, WebConstants.PATH_TASKS);
  }

  @Test
  @DisplayName("echoes a client identifier that is short and safely spelled")
  void echoesASafeIdentifier() throws IOException {
    FakeExchange exchange =
        handled(request().withRequestHeader(WebConstants.HEADER_REQUEST_ID, SAFE));

    assertThat(exchange.responseHeader(WebConstants.HEADER_REQUEST_ID)).isEqualTo(SAFE);
    assertThat(seenInContext).containsExactly(SAFE);
  }

  @Test
  @DisplayName("accepts an identifier of exactly the maximum length")
  void acceptsTheMaximumLength() throws IOException {
    FakeExchange exchange =
        handled(request().withRequestHeader(WebConstants.HEADER_REQUEST_ID, AT_THE_LIMIT));

    assertThat(exchange.responseHeader(WebConstants.HEADER_REQUEST_ID)).isEqualTo(AT_THE_LIMIT);
  }

  @ParameterizedTest
  @DisplayName("replaces anything a caller could use to forge a log line or pad the log")
  @ValueSource(strings = {"has a space", "semi;colon", "quote\"mark", "brack[et", "", "a b"})
  void replacesAnUnsafeIdentifier(String unsafe) throws IOException {
    FakeExchange exchange =
        handled(request().withRequestHeader(WebConstants.HEADER_REQUEST_ID, unsafe));

    assertThat(exchange.responseHeader(WebConstants.HEADER_REQUEST_ID)).isNotEqualTo(unsafe);
    assertThat(exchange.responseHeader(WebConstants.HEADER_REQUEST_ID)).isNotEmpty();
  }

  @Test
  void replacesAnIdentifierPastTheMaximumLength() throws IOException {
    FakeExchange exchange =
        handled(request().withRequestHeader(WebConstants.HEADER_REQUEST_ID, TOO_LONG));

    assertThat(exchange.responseHeader(WebConstants.HEADER_REQUEST_ID)).isNotEqualTo(TOO_LONG);
  }

  @Test
  @DisplayName("mints an identifier when the client supplied none")
  void mintsAnIdentifierWhenNoneArrived() throws IOException {
    FakeExchange exchange = handled(request());

    assertThat(exchange.responseHeader(WebConstants.HEADER_REQUEST_ID)).isNotEmpty();
    assertThat(seenInContext).singleElement().isNotNull();
  }

  @Test
  @DisplayName("clears the logging context, so the next request is not stamped with this one")
  void clearsTheLoggingContext() throws IOException {
    handled(request().withRequestHeader(WebConstants.HEADER_REQUEST_ID, SAFE));

    assertThat(MDC.get(WebConstants.HEADER_REQUEST_ID)).isNull();
  }

  @Test
  @DisplayName("clears the logging context even when the handler failed")
  void clearsTheLoggingContextAfterAFailure() {
    FakeExchange exchange = request();

    try {
      new RequestIdHandler(
              ignored -> {
                throw new IllegalStateException(PASSED_THROUGH);
              })
          .handle(exchange);
    } catch (IllegalStateException | IOException expected) {
      // The handler is expected to propagate; what matters is the context afterwards.
    }

    assertThat(MDC.get(WebConstants.HEADER_REQUEST_ID)).isNull();
  }
}
