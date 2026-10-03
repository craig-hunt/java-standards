package com.sagecrest.standards.web.endpoints;

import com.sagecrest.standards.application.health.HealthService;
import com.sagecrest.standards.application.health.ReadinessCheck;
import com.sagecrest.standards.domain.health.HealthConstants;
import com.sagecrest.standards.domain.health.HealthReport;
import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.errors.Problem;
import com.sagecrest.standards.web.routing.Responses;
import com.sagecrest.standards.web.routing.Route;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Liveness and readiness, and the only routes that answer without a token.
 *
 * <p>They say different things. Liveness answers as long as the process can serve a request, so a
 * platform restarts an instance only when it has actually stopped working. Readiness answers for
 * the dependencies, so a platform stops sending traffic to an instance whose database has gone away
 * without killing a process that would recover when it comes back. Collapsing the two into one
 * endpoint turns a database outage into a restart loop.
 *
 * <p>Readiness logs the failure and answers with a generic body. The caller is a platform probe,
 * which acts on the status and has no use for the reason; the operator needs the reason, and the
 * log is where the operator looks.
 */
public final class HealthEndpoints {

  private static final Logger LOG = LoggerFactory.getLogger(HealthEndpoints.class);

  private final HealthService service;

  public HealthEndpoints(HealthService service) {
    this.service = service;
  }

  public List<Route> routes() {
    return List.of(
        Route.get(WebConstants.PATH_HEALTH_READY, this::ready),
        Route.get(WebConstants.PATH_HEALTH, this::live));
  }

  private void live(HttpExchange exchange, List<String> captured) throws IOException {
    Responses.json(exchange, WebConstants.STATUS_OK, new HealthReport(HealthConstants.STATUS_OK));
  }

  private void ready(HttpExchange exchange, List<String> captured) throws IOException {
    ReadinessCheck outcome = service.check();
    if (outcome.ready()) {
      Responses.json(exchange, WebConstants.STATUS_OK, new HealthReport(HealthConstants.STATUS_OK));
      return;
    }
    outcome.failure().ifPresent(failure -> LOG.error(HealthConstants.MSG_NOT_READY, failure));
    Responses.problem(
        exchange,
        Problem.of(
            WebConstants.STATUS_UNAVAILABLE,
            WebConstants.TITLE_UNAVAILABLE,
            HealthConstants.STATUS_UNAVAILABLE,
            WebConstants.MSG_NOT_READY));
  }
}
