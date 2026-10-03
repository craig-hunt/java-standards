package com.sagecrest.standards.web.endpoints;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.application.health.HealthService;
import com.sagecrest.standards.application.ports.HealthProbe;
import com.sagecrest.standards.domain.health.HealthConstants;
import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.routing.FakeExchange;
import com.sagecrest.standards.web.routing.Router;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HealthEndpointsTest {

  private static final String DATABASE_DOWN = "connection refused";
  private static final String STATUS_MEMBER = "\"status\":\"ok\"";

  private ExecutorService probes;

  @BeforeEach
  void setUp() {
    probes = Executors.newVirtualThreadPerTaskExecutor();
  }

  @AfterEach
  void tearDown() {
    probes.shutdownNow();
  }

  private FakeExchange answered(String path, HealthProbe probe) throws IOException {
    FakeExchange exchange = new FakeExchange(WebConstants.METHOD_GET, path);
    new Router(new HealthEndpoints(new HealthService(probe, probes)).routes()).handle(exchange);
    return exchange;
  }

  @Test
  @DisplayName("liveness answers while the process can serve, whatever the database is doing")
  void livenessIgnoresTheDatabase() throws IOException {
    FakeExchange exchange =
        answered(
            WebConstants.PATH_HEALTH,
            () -> {
              throw new IllegalStateException(DATABASE_DOWN);
            });

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_OK);
    assertThat(exchange.responseText()).contains(STATUS_MEMBER);
  }

  @Test
  void readinessAnswersWhenTheDependencyAnswers() throws IOException {
    FakeExchange exchange = answered(WebConstants.PATH_HEALTH_READY, () -> {});

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_OK);
    assertThat(exchange.responseText()).contains(STATUS_MEMBER);
  }

  @Test
  @DisplayName("readiness reports unavailable, and tells a probe nothing about the reason")
  void readinessReportsUnavailableWithoutTheReason() throws IOException {
    FakeExchange exchange =
        answered(
            WebConstants.PATH_HEALTH_READY,
            () -> {
              throw new IllegalStateException(DATABASE_DOWN);
            });

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_UNAVAILABLE);
    assertThat(exchange.responseHeader(WebConstants.HEADER_CONTENT_TYPE))
        .isEqualTo(WebConstants.CONTENT_TYPE_PROBLEM);
    assertThat(exchange.responseText())
        .contains(HealthConstants.STATUS_UNAVAILABLE, WebConstants.MSG_NOT_READY);
    assertThat(exchange.responseText())
        .as("the operator reads the reason in the log, not the probe in the body")
        .doesNotContain(DATABASE_DOWN);
  }

  @Test
  @DisplayName("the two paths are different routes, so neither answers for the other")
  void theTwoPathsAreDifferentRoutes() throws IOException {
    assertThat(answered(WebConstants.PATH_HEALTH, () -> {}).status())
        .isEqualTo(WebConstants.STATUS_OK);
    assertThat(
            answered(
                    WebConstants.PATH_HEALTH_READY,
                    () -> {
                      throw new IllegalStateException(DATABASE_DOWN);
                    })
                .status())
        .isEqualTo(WebConstants.STATUS_UNAVAILABLE);
  }
}
