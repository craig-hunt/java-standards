package com.sagecrest.standards.web.endpoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.application.tasks.TaskService;
import com.sagecrest.standards.domain.errors.NotFoundException;
import com.sagecrest.standards.domain.errors.ValidationException;
import com.sagecrest.standards.domain.tasks.TaskConstants;
import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.routing.FakeExchange;
import com.sagecrest.standards.web.routing.Router;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Drives the task routes without a socket or a database.
 *
 * <p>No failure mapper sits in the chain, so a rejected request surfaces here as the exception the
 * endpoint raised. That is the contract: an endpoint decides nothing about status codes, and a test
 * that went through the mapper would be testing the mapper again.
 */
class TaskEndpointsTest {

  private static final String FIRST = "Read the ADR";
  private static final String SECOND = "Answer the questionnaire";
  private static final String NEW_TITLE = "Draft the runbook";

  private static final String TASK_ONE = "/api/tasks/1";
  private static final String TASK_ABSENT = "/api/tasks/99";
  private static final String TASK_UNREADABLE = "/api/tasks/not-a-number";
  private static final String FILTER_ACTIVE = "/api/tasks?filter=active";
  private static final String FILTER_UNKNOWN = "/api/tasks?filter=archived";

  private static final String BODY_CREATE = "{\"title\":\"Draft the runbook\"}";
  private static final String BODY_BLANK_TITLE = "{\"title\":\"   \"}";
  private static final String BODY_COMPLETED = "{\"completed\":true}";
  private static final String BODY_NO_FLAG = "{}";

  private static final int NO_BODY = -1;

  // The exact members a client reads. Spelled out rather than built from the contract,
  // because the point of these assertions is that the wire shape has not changed.
  private static final String ONE_REMAINING = "\"remaining\":1";
  private static final String TWO_IN_TOTAL = "\"total\":2";
  private static final String NOT_COMPLETED = "\"completed\":false";
  private static final String IS_COMPLETED = "\"completed\":true";
  private static final String ONE_REMOVED = "\"removed\":1";

  private FakeTaskStore store;
  private Router router;

  @BeforeEach
  void setUp() {
    store = new FakeTaskStore();
    router = new Router(new TaskEndpoints(new TaskService(store)).routes());
  }

  private FakeExchange handled(FakeExchange exchange) throws IOException {
    router.handle(exchange);
    return exchange;
  }

  private static FakeExchange get(String path) {
    return new FakeExchange(WebConstants.METHOD_GET, path);
  }

  @Test
  @DisplayName("lists with counts across every task, not across the filtered view")
  void listsWithCountsAcrossEveryTask() throws IOException {
    store.given(FIRST, false);
    store.given(SECOND, true);

    FakeExchange exchange = handled(get(FILTER_ACTIVE));

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_OK);
    assertThat(exchange.responseText()).contains(FIRST).doesNotContain(SECOND);
    assertThat(exchange.responseText()).contains(ONE_REMAINING, TWO_IN_TOTAL);
  }

  @Test
  void refusesAFilterItDoesNotPublish() {
    assertThatThrownBy(() -> handled(get(FILTER_UNKNOWN)))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).code())
        .isEqualTo(TaskConstants.CODE_INVALID_FILTER);
  }

  @Test
  @DisplayName("answers a created task with 201 rather than 200")
  void answersACreatedTaskWith201() throws IOException {
    FakeExchange exchange =
        handled(new FakeExchange(WebConstants.METHOD_POST, WebConstants.PATH_TASKS, BODY_CREATE));

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_CREATED);
    assertThat(exchange.responseText()).contains(NEW_TITLE, NOT_COMPLETED);
  }

  @Test
  void refusesABlankTitleBeforeItReachesTheStore() {
    assertThatThrownBy(
            () ->
                handled(
                    new FakeExchange(
                        WebConstants.METHOD_POST, WebConstants.PATH_TASKS, BODY_BLANK_TITLE)))
        .isInstanceOf(ValidationException.class);

    assertThat(store.list()).isEmpty();
  }

  @Test
  void setsCompletion() throws IOException {
    store.given(FIRST, false);

    FakeExchange exchange =
        handled(new FakeExchange(WebConstants.METHOD_PATCH, TASK_ONE, BODY_COMPLETED));

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_OK);
    assertThat(exchange.responseText()).contains(IS_COMPLETED);
  }

  @Test
  @DisplayName("refuses an update that omits the flag rather than reading it as false")
  void refusesAnUpdateThatOmitsTheFlag() {
    store.given(FIRST, true);

    assertThatThrownBy(
            () -> handled(new FakeExchange(WebConstants.METHOD_PATCH, TASK_ONE, BODY_NO_FLAG)))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).fields())
        .isEqualTo(
            java.util.Map.of(TaskConstants.FIELD_COMPLETED, TaskConstants.MSG_COMPLETED_REQUIRED));
  }

  @Test
  @DisplayName("treats an identifier that names no task, and one that is not an identifier, alike")
  void treatsAnAbsentAndAnUnreadableIdentifierAlike() {
    assertThatThrownBy(
            () -> handled(new FakeExchange(WebConstants.METHOD_PATCH, TASK_ABSENT, BODY_COMPLETED)))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(
            () ->
                handled(
                    new FakeExchange(WebConstants.METHOD_PATCH, TASK_UNREADABLE, BODY_COMPLETED)))
        .isInstanceOf(NotFoundException.class)
        .extracting(failure -> ((NotFoundException) failure).code())
        .isEqualTo(TaskConstants.CODE_NOT_FOUND);
  }

  @Test
  @DisplayName("a delete answers with no body at all")
  void aDeleteAnswersWithNoBody() throws IOException {
    store.given(FIRST, false);

    FakeExchange exchange = handled(new FakeExchange(WebConstants.METHOD_DELETE, TASK_ONE));

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_NO_CONTENT);
    assertThat(exchange.declaredLength()).isEqualTo(NO_BODY);
    assertThat(exchange.responseText()).isEmpty();
    assertThat(store.list()).isEmpty();
  }

  @Test
  @DisplayName("clearing reports how many it removed, and leaves the rest")
  void clearingReportsHowManyItRemoved() throws IOException {
    store.given(FIRST, true);
    store.given(SECOND, false);

    FakeExchange exchange =
        handled(new FakeExchange(WebConstants.METHOD_DELETE, WebConstants.PATH_TASKS));

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_OK);
    assertThat(exchange.responseText()).contains(ONE_REMOVED);
    assertThat(store.list())
        .singleElement()
        .extracting(task -> task.title().value())
        .isEqualTo(SECOND);
  }
}
