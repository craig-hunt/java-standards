package com.sagecrest.standards.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagecrest.standards.domain.errors.ErrorConstants;
import com.sagecrest.standards.domain.health.HealthConstants;
import com.sagecrest.standards.domain.inventory.InventoryConstants;
import com.sagecrest.standards.domain.signups.SignupConstants;
import com.sagecrest.standards.domain.tasks.TaskConstants;
import com.sagecrest.standards.infrastructure.persistence.ConnectionPool;
import com.sagecrest.standards.infrastructure.persistence.Database;
import com.sagecrest.standards.infrastructure.persistence.Schema;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * The service, driven over HTTP the way a client drives it.
 *
 * <p>A real server on a real port against a real PostgreSQL. Everything this suite asserts is
 * something a unit test cannot see: that authentication wraps the context rather than the handler,
 * that a domain failure arrives as the status and the body RFC 9457 describes, and that a route's
 * JSON shape is what the contract says it is.
 */
class ServiceIT {

  private static final String IMAGE = "postgres:16-alpine";
  private static final String TOKEN = "a-test-token";
  private static final String WRONG_TOKEN = "not-the-token";
  private static final String EPHEMERAL_PORT = "0";

  private static final String TITLE = "Read the ADR";
  private static final String BLANK_TITLE = "   ";
  private static final String NAME = "Ada Lovelace";
  private static final String EMAIL = "ada@example.com";

  private static final String LOCALHOST = "http://localhost:%d%s";
  private static final String BEARER = "Bearer %s";
  private static final String UNKNOWN_PATH = "/api/nowhere";
  private static final String ABSENT_TASK = "/api/tasks/999999";
  private static final String UNREADABLE_TASK = "/api/tasks/not-a-number";
  private static final String TASK_BY_ID = "/api/tasks/%d";
  private static final String BAD_SORT = "/api/inventory?sort=colour";
  private static final String SEARCH_BADGE = "/api/inventory?search=BADGE";
  private static final String FILTER_COMPLETED = "/api/tasks?filter=completed";

  private static final String SAFE_REQUEST_ID = "abc-123_4.5";

  /** A space is enough to fail the check, and unlike a newline it survives an HTTP header. */
  private static final String UNSAFE_REQUEST_ID = "has a space";

  private static final String FIELD_CODE = "code";
  private static final String FIELD_FIELDS = "fields";
  private static final String FIELD_STATUS = "status";
  private static final String FIELD_TITLE = "title";
  private static final String FIELD_DETAIL = "detail";
  private static final String FIELD_TYPE = "type";
  private static final String FIELD_ID = "id";
  private static final String FIELD_COMPLETED = "completed";
  private static final String FIELD_TASKS = "tasks";
  private static final String FIELD_REMAINING = "remaining";
  private static final String FIELD_TOTAL = "total";
  private static final String FIELD_SHOWN = "shown";
  private static final String FIELD_ITEMS = "items";
  private static final String FIELD_REMOVED = "removed";
  private static final String FIELD_SUMMARY = "summary";

  private static final String TYPE_BLANK = "about:blank";
  private static final String METHOD_PUT = "PUT";
  private static final String NO_BODY = "";

  private static final String BODY_TASK = "{\"title\":\"%s\"}";
  private static final String BODY_COMPLETED = "{\"completed\":%b}";
  private static final String BODY_EMPTY_OBJECT = "{}";
  private static final String BODY_UNKNOWN_MEMBER = "{\"title\":\"x\",\"colour\":\"red\"}";
  private static final String BODY_NOT_JSON = "{";
  private static final String BODY_TWO_OBJECTS = "{\"title\":\"a\"}{\"title\":\"b\"}";
  private static final String BODY_JSON_NULL = "null";
  private static final String BODY_SIGNUP =
      """
      {"fullName":"%s","email":"%s","plan":"%s","seats":2,"notes":"","acceptTerms":true}\
      """;
  private static final String BODY_SIGNUP_EMPTY =
      """
      {"fullName":"","email":"","plan":"","seats":0,"notes":"","acceptTerms":false}\
      """;

  private static final int EXPECTED_PROBLEMS = 5;
  private static final int ONE = 1;
  private static final int TWO = 2;
  private static final int TEN = 10;
  private static final int ZERO = 0;

  private static final ObjectMapper JSON = new ObjectMapper();

  /**
   * A deadline on the whole request, not only on the connection.
   *
   * <p>A connect timeout alone leaves {@code send} waiting forever on a server that accepted the
   * connection and then never answered. Under mutation analysis that is not hypothetical: a mutant
   * that breaks the response write produces exactly that, the test hangs, and PIT scores the
   * timeout as a kill. The gate then reports a healthy number for mutants nothing actually caught.
   *
   * <p>Generous rather than tight. A first request against a cold, instrumented JVM takes far
   * longer than a warm one, and a deadline sized for the warm case fails the analysis run itself.
   * The point is to bound a hang, not to assert a latency.
   */
  private static final Duration REQUEST_DEADLINE = Duration.ofSeconds(TEN);

  private static final HttpClient CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TWO)).build();

  private static final int PORT;

  /**
   * One container and one server for the whole run, started in a static block.
   *
   * <p>Not {@code @BeforeAll} with a matching {@code @AfterAll}. That shape starts a database and
   * an HTTP server once per class per JVM, which is fine under Surefire and ruinous under PIT: a
   * mutation analysis restarts the JVM repeatedly, and paying for a container each time made every
   * minion exceed its deadline. PIT scores a timed-out mutant as killed, so the gate reported a
   * healthy number for a module it had barely tested. Starting once and never stopping removes the
   * cost; Ryuk removes the container when the run ends, and the server dies with the JVM.
   */
  static {
    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(IMAGE);
    postgres.start();

    // The migrator runs before the API, exactly as compose arranges it in a deployment.
    // The service no longer touches the schema, so a test that skipped this step would
    // fail on the first query rather than silently passing.
    try (HikariDataSource migration =
        ConnectionPool.open(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
      new Schema(new Database(migration)).apply();
    }

    Service service = started(postgres);
    PORT = service.port();
  }

  private static Service started(PostgreSQLContainer<?> postgres) {
    try {
      Service service =
          new Service(
              Settings.from(
                  name ->
                      switch (name) {
                        case WebConstants.ENV_DATABASE_URL -> postgres.getJdbcUrl();
                        case WebConstants.ENV_DATABASE_USER -> postgres.getUsername();
                        case WebConstants.ENV_DATABASE_PASSWORD -> postgres.getPassword();
                        case WebConstants.ENV_API_TOKEN -> TOKEN;
                        case WebConstants.ENV_PORT -> EPHEMERAL_PORT;
                        default -> null;
                      }));
      service.start();
      return service;
    } catch (IOException cannotBind) {
      throw new IllegalStateException(cannotBind);
    }
  }

  @Test
  @DisplayName("answers liveness without a token, because a probe carries none")
  void answersLivenessWithoutAToken() throws Exception {
    HttpResponse<String> response = anonymous(WebConstants.PATH_HEALTH);

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_OK);
    assertThat(body(response).get(FIELD_STATUS).asText()).isEqualTo(HealthConstants.STATUS_OK);
  }

  @Test
  void answersReadinessWhenTheDatabaseIsThere() throws Exception {
    HttpResponse<String> response = anonymous(WebConstants.PATH_HEALTH_READY);

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_OK);
    assertThat(body(response).get(FIELD_STATUS).asText()).isEqualTo(HealthConstants.STATUS_OK);
  }

  @Test
  @DisplayName("refuses an API request with no token, and says how to present one")
  void refusesARequestWithNoToken() throws Exception {
    HttpResponse<String> response = anonymous(WebConstants.PATH_TASKS);

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_UNAUTHORIZED);
    assertThat(response.headers().firstValue(WebConstants.HEADER_WWW_AUTHENTICATE))
        .contains(WebConstants.CHALLENGE_BEARER);
    assertThat(contentType(response)).isEqualTo(WebConstants.CONTENT_TYPE_PROBLEM);
    assertThat(body(response).get(FIELD_CODE).asText()).isEqualTo(WebConstants.CODE_UNAUTHORIZED);
  }

  @Test
  void refusesARequestWithTheWrongToken() throws Exception {
    HttpResponse<String> response =
        send(
            request(WebConstants.PATH_TASKS)
                .header(WebConstants.HEADER_AUTHORIZATION, BEARER.formatted(WRONG_TOKEN))
                .GET());

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_UNAUTHORIZED);
  }

  @Test
  @DisplayName("creates a task and answers with the shape the contract declares")
  void createsATask() throws Exception {
    HttpResponse<String> response = post(WebConstants.PATH_TASKS, BODY_TASK.formatted(TITLE));

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_CREATED);
    assertThat(contentType(response)).isEqualTo(WebConstants.CONTENT_TYPE_JSON);
    JsonNode created = body(response);
    assertThat(created.get(FIELD_ID).asLong()).isPositive();
    assertThat(created.get(FIELD_TITLE).asText()).isEqualTo(TITLE);
    assertThat(created.get(FIELD_COMPLETED).asBoolean()).isFalse();
  }

  @Test
  @DisplayName("answers a blank title with 422 and the field the message belongs to")
  void answersABlankTitleWithTheFieldItBelongsTo() throws Exception {
    HttpResponse<String> response = post(WebConstants.PATH_TASKS, BODY_TASK.formatted(BLANK_TITLE));

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_UNPROCESSABLE);
    assertThat(contentType(response)).isEqualTo(WebConstants.CONTENT_TYPE_PROBLEM);
    JsonNode problem = body(response);
    assertThat(problem.get(FIELD_TYPE).asText()).isEqualTo(TYPE_BLANK);
    assertThat(problem.get(FIELD_TITLE).asText()).isEqualTo(WebConstants.TITLE_UNPROCESSABLE);
    assertThat(problem.get(FIELD_STATUS).asInt()).isEqualTo(WebConstants.STATUS_UNPROCESSABLE);
    assertThat(problem.get(FIELD_DETAIL).asText()).isEqualTo(ErrorConstants.MSG_VALIDATION);
    assertThat(problem.get(FIELD_CODE).asText()).isEqualTo(ErrorConstants.CODE_VALIDATION);
    assertThat(problem.get(FIELD_FIELDS).get(TaskConstants.FIELD_TITLE).asText())
        .isEqualTo(TaskConstants.MSG_TITLE_REQUIRED);
  }

  @Test
  @DisplayName("counts across every task while the filter narrows what it lists")
  void countsAcrossEveryTask() throws Exception {
    long first = idFrom(post(WebConstants.PATH_TASKS, BODY_TASK.formatted(TITLE)));
    post(WebConstants.PATH_TASKS, BODY_TASK.formatted(TITLE));
    patch(TASK_BY_ID.formatted(first), BODY_COMPLETED.formatted(true));

    JsonNode view = body(get(FILTER_COMPLETED));

    assertThat(view.get(FIELD_TASKS)).hasSize(ONE);
    assertThat(view.get(FIELD_REMAINING).asInt()).isPositive();
    assertThat(view.get(FIELD_TOTAL).asInt()).isGreaterThanOrEqualTo(TWO);
  }

  @Test
  @DisplayName("refuses an update that omits the completion flag, rather than reading it as false")
  void refusesAnUpdateThatOmitsTheFlag() throws Exception {
    long id = idFrom(post(WebConstants.PATH_TASKS, BODY_TASK.formatted(TITLE)));

    HttpResponse<String> response = patch(TASK_BY_ID.formatted(id), BODY_EMPTY_OBJECT);

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_UNPROCESSABLE);
    assertThat(body(response).get(FIELD_FIELDS).get(TaskConstants.FIELD_COMPLETED).asText())
        .isEqualTo(TaskConstants.MSG_COMPLETED_REQUIRED);
  }

  @Test
  void deletesATaskAndAnswersWithNoBody() throws Exception {
    long id = idFrom(post(WebConstants.PATH_TASKS, BODY_TASK.formatted(TITLE)));

    HttpResponse<String> response = delete(TASK_BY_ID.formatted(id));

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_NO_CONTENT);
    assertThat(response.body()).isEmpty();
  }

  @Test
  @DisplayName("reports how many completed tasks a clear removed")
  void reportsHowManyAClearRemoved() throws Exception {
    long id = idFrom(post(WebConstants.PATH_TASKS, BODY_TASK.formatted(TITLE)));
    patch(TASK_BY_ID.formatted(id), BODY_COMPLETED.formatted(true));

    assertThat(body(delete(WebConstants.PATH_TASKS)).get(FIELD_REMOVED).asLong()).isPositive();
    assertThat(body(delete(WebConstants.PATH_TASKS)).get(FIELD_REMOVED).asLong()).isEqualTo(ZERO);
  }

  @Test
  @DisplayName("answers an identifier that names no task, and one that is not an identifier, alike")
  void answersAnUnknownTaskWith404() throws Exception {
    assertThat(patch(ABSENT_TASK, BODY_COMPLETED.formatted(true)).statusCode())
        .isEqualTo(WebConstants.STATUS_NOT_FOUND);
    assertThat(patch(UNREADABLE_TASK, BODY_COMPLETED.formatted(true)).statusCode())
        .isEqualTo(WebConstants.STATUS_NOT_FOUND);
    assertThat(delete(ABSENT_TASK).statusCode()).isEqualTo(WebConstants.STATUS_NOT_FOUND);
  }

  @Test
  void recordsASignupAndSummarizesIt() throws Exception {
    HttpResponse<String> response =
        post(
            WebConstants.PATH_SIGNUPS,
            BODY_SIGNUP.formatted(NAME, EMAIL, SignupConstants.PLAN_GROWTH));

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_CREATED);
    JsonNode confirmation = body(response);
    assertThat(confirmation.get(FIELD_ID).asLong()).isPositive();
    assertThat(confirmation.get(FIELD_SUMMARY).asText())
        .contains(NAME, SignupConstants.PLAN_GROWTH);
  }

  @Test
  @DisplayName("reports every signup problem at once, keyed by the names the client sent")
  void reportsEverySignupProblemAtOnce() throws Exception {
    HttpResponse<String> response = post(WebConstants.PATH_SIGNUPS, BODY_SIGNUP_EMPTY);

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_UNPROCESSABLE);
    JsonNode fields = body(response).get(FIELD_FIELDS);
    assertThat(fields).hasSize(EXPECTED_PROBLEMS);
    assertThat(fields.get(SignupConstants.FIELD_FULL_NAME).asText())
        .isEqualTo(SignupConstants.MSG_NAME_REQUIRED);
    assertThat(fields.get(SignupConstants.FIELD_ACCEPT_TERMS).asText())
        .isEqualTo(SignupConstants.MSG_TERMS_REQUIRED);
  }

  @Test
  void answersTheSeededStockRows() throws Exception {
    JsonNode stock = body(get(WebConstants.PATH_INVENTORY));

    assertThat(stock.get(FIELD_ITEMS)).isNotEmpty();
    assertThat(stock.get(FIELD_TOTAL).asInt()).isEqualTo(stock.get(FIELD_SHOWN).asInt());
  }

  @Test
  void narrowsStockToTheSearchRegardlessOfCase() throws Exception {
    JsonNode stock = body(get(SEARCH_BADGE));

    assertThat(stock.get(FIELD_SHOWN).asInt()).isEqualTo(ONE);
    assertThat(stock.get(FIELD_TOTAL).asInt()).isGreaterThan(ONE);
  }

  @Test
  void refusesAColumnTheTableCannotSortOn() throws Exception {
    HttpResponse<String> response = get(BAD_SORT);

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_UNPROCESSABLE);
    assertThat(body(response).get(FIELD_CODE).asText())
        .isEqualTo(InventoryConstants.CODE_INVALID_QUERY);
  }

  @Test
  @DisplayName("answers an unknown JSON member with 400, so a misspelling is discoverable")
  void answersAnUnknownJsonMemberWith400() throws Exception {
    HttpResponse<String> response = post(WebConstants.PATH_TASKS, BODY_UNKNOWN_MEMBER);

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_BAD_REQUEST);
    assertThat(body(response).get(FIELD_CODE).asText()).isEqualTo(ErrorConstants.CODE_INVALID_BODY);
  }

  @Test
  void answersAnUnparseableBodyWith400() throws Exception {
    assertThat(post(WebConstants.PATH_TASKS, BODY_NOT_JSON).statusCode())
        .isEqualTo(WebConstants.STATUS_BAD_REQUEST);
  }

  @Test
  @DisplayName("refuses a second object after the first rather than binding one and dropping one")
  void refusesTrailingContent() throws Exception {
    HttpResponse<String> response = post(WebConstants.PATH_TASKS, BODY_TWO_OBJECTS);

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_BAD_REQUEST);
    assertThat(body(response).get(FIELD_CODE).asText()).isEqualTo(ErrorConstants.CODE_INVALID_BODY);
  }

  @Test
  @DisplayName("treats a body of JSON null as malformed, not as a server fault")
  void refusesAJsonNullBody() throws Exception {
    HttpResponse<String> response = post(WebConstants.PATH_TASKS, BODY_JSON_NULL);

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_BAD_REQUEST);
    assertThat(body(response).get(FIELD_CODE).asText()).isEqualTo(ErrorConstants.CODE_INVALID_BODY);
  }

  @Test
  @DisplayName("answers a path nobody serves with the same problem shape as everything else")
  void answersAnUnknownPathWithAProblem() throws Exception {
    HttpResponse<String> response = get(UNKNOWN_PATH);

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_NOT_FOUND);
    assertThat(contentType(response)).isEqualTo(WebConstants.CONTENT_TYPE_PROBLEM);
    assertThat(body(response).get(FIELD_CODE).asText()).isEqualTo(WebConstants.CODE_NOT_FOUND);
  }

  @Test
  @DisplayName("tells a wrong method apart from a wrong path, and names the methods that work")
  void tellsAWrongMethodApartFromAWrongPath() throws Exception {
    HttpResponse<String> response =
        send(
            authorized(WebConstants.PATH_TASKS)
                .method(METHOD_PUT, HttpRequest.BodyPublishers.ofString(NO_BODY)));

    assertThat(response.statusCode()).isEqualTo(WebConstants.STATUS_METHOD_NOT_ALLOWED);
    assertThat(response.headers().firstValue(WebConstants.HEADER_ALLOW))
        .get()
        .asString()
        .contains(WebConstants.METHOD_GET, WebConstants.METHOD_POST, WebConstants.METHOD_DELETE);
  }

  @Test
  @DisplayName("echoes a safe request identifier and replaces one that is not")
  void echoesOnlyASafeRequestIdentifier() throws Exception {
    HttpResponse<String> echoed =
        send(
            authorized(WebConstants.PATH_TASKS)
                .header(WebConstants.HEADER_REQUEST_ID, SAFE_REQUEST_ID)
                .GET());
    HttpResponse<String> replaced =
        send(
            authorized(WebConstants.PATH_TASKS)
                .header(WebConstants.HEADER_REQUEST_ID, UNSAFE_REQUEST_ID)
                .GET());

    assertThat(echoed.headers().firstValue(WebConstants.HEADER_REQUEST_ID))
        .contains(SAFE_REQUEST_ID);
    assertThat(replaced.headers().firstValue(WebConstants.HEADER_REQUEST_ID))
        .get()
        .asString()
        .isNotEqualTo(UNSAFE_REQUEST_ID);
  }

  @Test
  @DisplayName("stamps every response with a request identifier, including a rejected one")
  void stampsEveryResponseWithARequestIdentifier() throws Exception {
    assertThat(
            anonymous(WebConstants.PATH_TASKS).headers().firstValue(WebConstants.HEADER_REQUEST_ID))
        .isPresent();
  }

  private static HttpRequest.Builder request(String path) {
    return HttpRequest.newBuilder(URI.create(LOCALHOST.formatted(PORT, path)))
        .timeout(REQUEST_DEADLINE);
  }

  private static HttpRequest.Builder authorized(String path) {
    return request(path)
        .header(WebConstants.HEADER_AUTHORIZATION, BEARER.formatted(TOKEN))
        .header(WebConstants.HEADER_CONTENT_TYPE, WebConstants.CONTENT_TYPE_JSON);
  }

  private static HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
    return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static HttpResponse<String> anonymous(String path) throws Exception {
    return send(request(path).GET());
  }

  private static HttpResponse<String> get(String path) throws Exception {
    return send(authorized(path).GET());
  }

  private static HttpResponse<String> post(String path, String payload) throws Exception {
    return send(authorized(path).POST(HttpRequest.BodyPublishers.ofString(payload)));
  }

  private static HttpResponse<String> patch(String path, String payload) throws Exception {
    return send(
        authorized(path)
            .method(WebConstants.METHOD_PATCH, HttpRequest.BodyPublishers.ofString(payload)));
  }

  private static HttpResponse<String> delete(String path) throws Exception {
    return send(authorized(path).DELETE());
  }

  private static JsonNode body(HttpResponse<String> response) throws IOException {
    return JSON.readTree(response.body());
  }

  private static long idFrom(HttpResponse<String> response) throws IOException {
    return body(response).get(FIELD_ID).asLong();
  }

  private static String contentType(HttpResponse<String> response) {
    return response.headers().firstValue(WebConstants.HEADER_CONTENT_TYPE).orElseThrow();
  }
}
