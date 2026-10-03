package com.sagecrest.standards.web.endpoints;

import com.sagecrest.standards.application.signups.SignupService;
import com.sagecrest.standards.domain.signups.SignupErrors;
import com.sagecrest.standards.domain.signups.SignupRequest;
import com.sagecrest.standards.domain.signups.SignupValidation;
import com.sagecrest.standards.domain.signups.SignupValidator;
import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.contracts.Contracts;
import com.sagecrest.standards.web.json.Json;
import com.sagecrest.standards.web.routing.Responses;
import com.sagecrest.standards.web.routing.Route;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;

/**
 * The signup route.
 *
 * <p>The request binds straight to the domain's own {@link SignupRequest} rather than to a contract
 * of this module's. That is the one place a domain type is allowed on the wire, and it is
 * deliberate: a validation failure answers with problems keyed by field name, and those keys have
 * to be the names the client sent. A separate contract would let the two sets of names drift, and
 * the drift would surface as a form that cannot find the input a message belongs to.
 */
public final class SignupEndpoints {

  private final SignupService service;

  public SignupEndpoints(SignupService service) {
    this.service = service;
  }

  public List<Route> routes() {
    return List.of(Route.post(WebConstants.PATH_SIGNUPS, this::create));
  }

  private void create(HttpExchange exchange, List<String> captured) throws IOException {
    SignupRequest submitted = Json.read(exchange.getRequestBody(), SignupRequest.class);
    SignupValidation validated = SignupValidator.validate(submitted);
    if (!validated.valid()) {
      throw SignupErrors.invalid(validated.problems());
    }
    Responses.json(
        exchange,
        WebConstants.STATUS_CREATED,
        Contracts.SignupResponse.from(service.create(validated.value().orElseThrow())));
  }
}
