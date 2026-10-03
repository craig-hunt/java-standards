package com.sagecrest.standards.web.auth;

import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.errors.Problem;
import com.sagecrest.standards.web.routing.Responses;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Requires a bearer token before anything downstream runs.
 *
 * <p>It wraps a whole context rather than each route. A per-route check is a check a new route can
 * forget, and the failure mode of forgetting is an open endpoint that nothing in the build notices.
 *
 * <p>The comparison runs through {@link MessageDigest#isEqual}, which does not stop at the first
 * differing byte. A plain {@code equals} leaks, through timing, how much of a guess was right,
 * which turns a brute-force search for the token from infeasible into linear in its length. The
 * length itself is still observable, and that is accepted: a token's length is not the secret.
 *
 * <p>The Go and C# siblings also accept a signed JWT here. Doing that in Java means taking on a JWT
 * library, and verifying a signature badly is worse than not offering the option, so this surface
 * accepts the static token only. The divergence is recorded rather than papered over.
 */
public final class BearerTokenHandler implements HttpHandler {

  private final HttpHandler next;
  private final byte[] expected;

  public BearerTokenHandler(HttpHandler next, String token) {
    this.next = next;
    this.expected = token.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!authorized(exchange)) {
      exchange
          .getResponseHeaders()
          .set(WebConstants.HEADER_WWW_AUTHENTICATE, WebConstants.CHALLENGE_BEARER);
      Responses.problem(
          exchange,
          Problem.of(
              WebConstants.STATUS_UNAUTHORIZED,
              WebConstants.TITLE_UNAUTHORIZED,
              WebConstants.CODE_UNAUTHORIZED,
              WebConstants.MSG_UNAUTHORIZED));
      return;
    }
    next.handle(exchange);
  }

  private boolean authorized(HttpExchange exchange) {
    String header = exchange.getRequestHeaders().getFirst(WebConstants.HEADER_AUTHORIZATION);
    if (header == null || !header.startsWith(WebConstants.SCHEME_BEARER_PREFIX)) {
      return false;
    }
    byte[] presented =
        header
            .substring(WebConstants.SCHEME_BEARER_PREFIX.length())
            .getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expected, presented);
  }
}
