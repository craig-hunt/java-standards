package com.sagecrest.standards.web.routing;

import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.errors.Problem;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Picks the route that answers a request.
 *
 * <p>It distinguishes a path nobody serves from a path that serves a different method, and answers
 * the second with 405 and an {@code Allow} header. Collapsing both into 404 would tell a client its
 * URL was wrong when its verb was.
 *
 * <p>Routes are tried in order, so a literal path registered before a parameterized one under the
 * same prefix wins. The ordering is the only thing resolving that overlap, which is why {@link
 * #handle} does not reorder them.
 */
public final class Router implements HttpHandler {

  private static final String ALLOW_SEPARATOR = ", ";

  private final List<Route> routes;

  public Router(List<Route> routes) {
    this.routes = List.copyOf(routes);
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    String method = exchange.getRequestMethod();

    List<Route> addressed = routes.stream().filter(route -> route.match(path).matches()).toList();
    if (addressed.isEmpty()) {
      Responses.problem(
          exchange,
          Problem.of(
              WebConstants.STATUS_NOT_FOUND,
              WebConstants.TITLE_NOT_FOUND,
              WebConstants.CODE_NOT_FOUND,
              WebConstants.MSG_NO_SUCH_ROUTE));
      return;
    }

    for (Route route : addressed) {
      if (route.method().equals(method)) {
        Matcher matched = route.match(path);
        matched.matches();
        route.handler().handle(exchange, Route.captures(matched));
        return;
      }
    }

    exchange.getResponseHeaders().set(WebConstants.HEADER_ALLOW, allowed(addressed));
    Responses.problem(
        exchange,
        Problem.of(
            WebConstants.STATUS_METHOD_NOT_ALLOWED,
            WebConstants.TITLE_METHOD_NOT_ALLOWED,
            WebConstants.CODE_METHOD_NOT_ALLOWED,
            WebConstants.MSG_METHOD_NOT_ALLOWED));
  }

  private static String allowed(List<Route> addressed) {
    return addressed.stream()
        .map(Route::method)
        .distinct()
        .reduce((a, b) -> a + ALLOW_SEPARATOR + b)
        .orElseThrow();
  }
}
