package com.sagecrest.standards.web.routing;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;

/**
 * What answers one route.
 *
 * <p>The captured path segments arrive as a list rather than the handler reparsing the URI, so the
 * pattern that decided the match is also the thing that extracts from it.
 */
@FunctionalInterface
public interface RouteHandler {

  void handle(HttpExchange exchange, List<String> captured) throws IOException;
}
