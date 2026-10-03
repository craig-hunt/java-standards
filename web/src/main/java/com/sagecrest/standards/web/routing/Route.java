package com.sagecrest.standards.web.routing;

import com.sagecrest.standards.web.WebConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One method and one path shape, with what answers them.
 *
 * <p>A pattern rather than a string with braces in it. A home-grown {@code {id}} syntax would need
 * its own parser, its own escaping rules, and its own tests, and it would answer the same question
 * a regular expression already answers.
 */
public record Route(String method, Pattern path, RouteHandler handler) {

  private static final String ANCHORED = "^%s$";
  private static final String SEGMENT = "([^/]+)";
  private static final String PATH_SEPARATOR = "/";

  public static Route get(String path, RouteHandler handler) {
    return new Route(WebConstants.METHOD_GET, exactly(path), handler);
  }

  public static Route post(String path, RouteHandler handler) {
    return new Route(WebConstants.METHOD_POST, exactly(path), handler);
  }

  public static Route delete(String path, RouteHandler handler) {
    return new Route(WebConstants.METHOD_DELETE, exactly(path), handler);
  }

  public static Route patchUnder(String path, RouteHandler handler) {
    return new Route(WebConstants.METHOD_PATCH, withSegment(path), handler);
  }

  public static Route deleteUnder(String path, RouteHandler handler) {
    return new Route(WebConstants.METHOD_DELETE, withSegment(path), handler);
  }

  public Matcher match(String requestPath) {
    return path.matcher(requestPath);
  }

  public static List<String> captures(Matcher matched) {
    List<String> values = new ArrayList<>(matched.groupCount());
    for (int group = 1; group <= matched.groupCount(); group++) {
      values.add(matched.group(group));
    }
    return List.copyOf(values);
  }

  private static Pattern exactly(String path) {
    return Pattern.compile(ANCHORED.formatted(Pattern.quote(path)));
  }

  private static Pattern withSegment(String prefix) {
    return Pattern.compile(ANCHORED.formatted(Pattern.quote(prefix + PATH_SEPARATOR) + SEGMENT));
  }
}
