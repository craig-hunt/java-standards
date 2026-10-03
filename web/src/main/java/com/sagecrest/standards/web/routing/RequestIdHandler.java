package com.sagecrest.standards.web.routing;

import com.sagecrest.standards.web.WebConstants;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

/**
 * Gives every request an identifier, and puts it on the response.
 *
 * <p>A client-supplied identifier is echoed only when it is short and made of characters safe in a
 * header and a log line. An unfiltered value travels into both: a newline would let a caller forge
 * log entries, and an unbounded one would let a caller choose how much of the log it occupies.
 * Anything that fails the check is replaced rather than rejected, because a bad header is no reason
 * to refuse the request.
 *
 * <p>The identifier goes into the logging context and is removed in a finally block. A thread that
 * kept it would stamp the next request with the previous request's identifier, and with virtual
 * threads there are far more threads to leak it across.
 */
public final class RequestIdHandler implements HttpHandler {

  private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]+");

  private final HttpHandler next;

  public RequestIdHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String id = identify(exchange);
    exchange.getResponseHeaders().set(WebConstants.HEADER_REQUEST_ID, id);
    MDC.put(WebConstants.HEADER_REQUEST_ID, id);
    try {
      next.handle(exchange);
    } finally {
      MDC.remove(WebConstants.HEADER_REQUEST_ID);
    }
  }

  private static String identify(HttpExchange exchange) {
    String supplied = exchange.getRequestHeaders().getFirst(WebConstants.HEADER_REQUEST_ID);
    boolean usable =
        supplied != null
            && !supplied.isEmpty()
            && supplied.length() <= WebConstants.MAX_REQUEST_ID_LENGTH
            && SAFE.matcher(supplied).matches();
    return usable ? supplied : UUID.randomUUID().toString();
  }
}
