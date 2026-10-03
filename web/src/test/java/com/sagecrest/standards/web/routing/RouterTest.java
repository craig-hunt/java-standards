package com.sagecrest.standards.web.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.web.WebConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RouterTest {

  private static final String ELSEWHERE = "/api/nowhere";
  private static final String UNDER = "/api/tasks/4";
  private static final String IDENTIFIER = "4";
  private static final String ALL_TASKS = "called for the whole collection";
  private static final String ONE_TASK = "called for one task";
  private static final String CLEARED = "called to clear";
  private static final String ALLOW_SEPARATOR = ", ";

  private final List<String> calls = new ArrayList<>();

  private RouteHandler records(String what) {
    return (exchange, captured) -> {
      calls.add(what);
      calls.addAll(captured);
    };
  }

  private Router router() {
    // Declared in the order the service declares them: the literal path before the
    // parameterized one, which is the only thing resolving the overlap between
    // DELETE /api/tasks and DELETE /api/tasks/4.
    return new Router(
        List.of(
            Route.get(WebConstants.PATH_TASKS, records(ALL_TASKS)),
            Route.delete(WebConstants.PATH_TASKS, records(CLEARED)),
            Route.deleteUnder(WebConstants.PATH_TASKS, records(ONE_TASK))));
  }

  @Test
  void dispatchesToTheRouteThatMatchesThePathAndMethod() throws IOException {
    router().handle(new FakeExchange(WebConstants.METHOD_GET, WebConstants.PATH_TASKS));

    assertThat(calls).containsExactly(ALL_TASKS);
  }

  @Test
  @DisplayName("hands the captured segment to the handler rather than making it reparse the URI")
  void handsTheCapturedSegmentToTheHandler() throws IOException {
    router().handle(new FakeExchange(WebConstants.METHOD_DELETE, UNDER));

    assertThat(calls).containsExactly(ONE_TASK, IDENTIFIER);
  }

  @Test
  @DisplayName("the literal route wins over the parameterized one declared after it")
  void theLiteralRouteWinsWhenDeclaredFirst() throws IOException {
    router().handle(new FakeExchange(WebConstants.METHOD_DELETE, WebConstants.PATH_TASKS));

    assertThat(calls).containsExactly(CLEARED);
  }

  @Test
  @DisplayName("a path nobody serves answers 404 with the problem shape")
  void aPathNobodyServesAnswers404() throws IOException {
    FakeExchange exchange = new FakeExchange(WebConstants.METHOD_GET, ELSEWHERE);

    router().handle(exchange);

    assertThat(calls).isEmpty();
    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_NOT_FOUND);
    assertThat(exchange.responseHeader(WebConstants.HEADER_CONTENT_TYPE))
        .isEqualTo(WebConstants.CONTENT_TYPE_PROBLEM);
    assertThat(exchange.responseText()).contains(WebConstants.CODE_NOT_FOUND);
  }

  @Test
  @DisplayName("a served path with the wrong method answers 405 and names the methods that work")
  void aWrongMethodAnswers405() throws IOException {
    FakeExchange exchange = new FakeExchange(WebConstants.METHOD_POST, WebConstants.PATH_TASKS);

    router().handle(exchange);

    assertThat(calls).isEmpty();
    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_METHOD_NOT_ALLOWED);
    assertThat(exchange.responseHeader(WebConstants.HEADER_ALLOW))
        .contains(WebConstants.METHOD_GET, WebConstants.METHOD_DELETE);
    assertThat(exchange.responseText()).contains(WebConstants.CODE_METHOD_NOT_ALLOWED);
  }

  @Test
  @DisplayName("the Allow header lists each method once, however many routes share it")
  void theAllowHeaderListsEachMethodOnce() throws IOException {
    FakeExchange exchange = new FakeExchange(WebConstants.METHOD_PATCH, WebConstants.PATH_TASKS);

    router().handle(exchange);

    assertThat(exchange.responseHeader(WebConstants.HEADER_ALLOW))
        .isEqualTo(WebConstants.METHOD_GET + ALLOW_SEPARATOR + WebConstants.METHOD_DELETE);
  }

  @Test
  @DisplayName("a router with no routes answers 404 rather than failing")
  void anEmptyRouterAnswers404() throws IOException {
    FakeExchange exchange = new FakeExchange(WebConstants.METHOD_GET, ELSEWHERE);

    new Router(List.of()).handle(exchange);

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_NOT_FOUND);
  }
}
