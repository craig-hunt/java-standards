package com.sagecrest.standards.web.routing;

import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.errors.Problem;
import com.sagecrest.standards.web.json.Json;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

/** The only place a response is written, so every response carries a content type and a length. */
public final class Responses {

  private static final int NO_BODY = -1;
  private static final int NOT_YET_SENT = -1;

  public static void json(HttpExchange exchange, int status, Object body) throws IOException {
    write(exchange, status, WebConstants.CONTENT_TYPE_JSON, Json.write(body));
  }

  public static void empty(HttpExchange exchange, int status) throws IOException {
    exchange.sendResponseHeaders(status, NO_BODY);
  }

  public static void problem(HttpExchange exchange, Problem problem) throws IOException {
    write(exchange, problem.status(), WebConstants.CONTENT_TYPE_PROBLEM, Json.write(problem));
  }

  /**
   * Whether a response has already begun.
   *
   * <p>The failure mapper asks before trying to write a problem. A handler that threw after sending
   * its headers cannot be given a different status, and attempting it replaces a truthful partial
   * response with an exception in the log and a connection reset at the client.
   */
  public static boolean alreadyStarted(HttpExchange exchange) {
    return exchange.getResponseCode() != NOT_YET_SENT;
  }

  private static void write(HttpExchange exchange, int status, String contentType, byte[] body)
      throws IOException {
    exchange.getResponseHeaders().set(WebConstants.HEADER_CONTENT_TYPE, contentType);
    exchange.sendResponseHeaders(status, body.length);
    try (OutputStream sink = exchange.getResponseBody()) {
      sink.write(body);
    }
  }

  private Responses() {}
}
