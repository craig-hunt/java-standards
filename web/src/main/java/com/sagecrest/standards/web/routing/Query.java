package com.sagecrest.standards.web.routing;

import com.sun.net.httpserver.HttpExchange;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The query string, read once into a map.
 *
 * <p>It decodes percent-escapes, which {@code URI.getQuery} also does, but {@code getQuery} decodes
 * the whole string before splitting it: an encoded {@code &} or {@code =} inside a value would then
 * split into extra parameters. Splitting first and decoding each half is the order that keeps a
 * search term containing an ampersand intact.
 */
public record Query(Map<String, String> values) {

  public Query {
    // Copied here as well as in the factory. The factory is the way a request
    // reaches this type, but the canonical constructor is public, and a record
    // whose invariant holds on only one of two paths has no invariant.
    values = Map.copyOf(values);
  }

  private static final String PAIR_SEPARATOR = "&";
  private static final String VALUE_SEPARATOR = "=";
  private static final int PAIR_LIMIT = 2;
  private static final int NAME = 0;
  private static final int VALUE = 1;
  private static final int NAME_ONLY = 1;

  public static Query of(HttpExchange exchange) {
    Map<String, String> values = new HashMap<>();
    String raw = exchange.getRequestURI().getRawQuery();
    if (raw == null || raw.isEmpty()) {
      return new Query(Map.of());
    }
    for (String pair : raw.split(PAIR_SEPARATOR)) {
      String[] halves = pair.split(VALUE_SEPARATOR, PAIR_LIMIT);
      String name = decode(halves[NAME]);
      values.put(name, halves.length == NAME_ONLY ? "" : decode(halves[VALUE]));
    }
    return new Query(Map.copyOf(values));
  }

  /** Answers null for an absent parameter, which is what the domain factories read as absent. */
  public String get(String name) {
    return values.get(name);
  }

  private static String decode(String encoded) {
    return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
  }
}
