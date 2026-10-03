package com.sagecrest.standards.web.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.web.WebConstants;
import java.util.List;
import java.util.regex.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RouteTest {

  private static final String PATH = "/api/tasks";
  private static final String UNDER = "/api/tasks/4";
  private static final String DEEPER = "/api/tasks/4/notes";
  private static final String PREFIXED = "/api/tasks-archive";
  private static final String SUFFIXED = "/prefix/api/tasks";
  private static final String IDENTIFIER = "4";
  private static final String NOT_A_NUMBER = "anything";
  private static final String UNDER_WITH_TEXT = "/api/tasks/anything";
  private static final String NO_CAPTURES = "/api/tasks";

  /** A path holding a character that means something to a regular expression. */
  private static final String WITH_A_DOT = "/api/a.b";

  /** The same path with the dot standing for any character, which must not match. */
  private static final String DOT_AS_WILDCARD = "/api/axb";

  private static final RouteHandler NOTHING = (exchange, captured) -> {};

  private static List<String> capturesFrom(Route route, String path) {
    Matcher matched = route.match(path);
    assertThat(matched.matches()).isTrue();
    return Route.captures(matched);
  }

  @Test
  @DisplayName("a literal route matches its own path and nothing around it")
  void aLiteralRouteMatchesOnlyItsOwnPath() {
    Route route = Route.get(PATH, NOTHING);

    assertThat(route.match(NO_CAPTURES).matches()).isTrue();
    assertThat(route.match(UNDER).matches()).as("not a path beneath it").isFalse();
    assertThat(route.match(PREFIXED).matches()).as("not a longer name").isFalse();
    assertThat(route.match(SUFFIXED).matches()).as("not a path that contains it").isFalse();
  }

  @Test
  void aLiteralRouteCapturesNothing() {
    assertThat(capturesFrom(Route.get(PATH, NOTHING), NO_CAPTURES)).isEmpty();
  }

  @Test
  @DisplayName("a parameterized route captures one segment and refuses two")
  void aParameterizedRouteCapturesOneSegment() {
    Route route = Route.patchUnder(PATH, NOTHING);

    assertThat(capturesFrom(route, UNDER)).containsExactly(IDENTIFIER);
    assertThat(route.match(DEEPER).matches()).as("one segment, not a subtree").isFalse();
    assertThat(route.match(PATH).matches()).as("the bare path is a different route").isFalse();
  }

  @Test
  @DisplayName("the segment captures any text, so the handler decides what names a resource")
  void theSegmentCapturesAnyText() {
    assertThat(capturesFrom(Route.deleteUnder(PATH, NOTHING), UNDER_WITH_TEXT))
        .containsExactly(NOT_A_NUMBER);
  }

  @Test
  @DisplayName("each factory carries the method it names")
  void eachFactoryCarriesItsMethod() {
    assertThat(Route.get(PATH, NOTHING).method()).isEqualTo(WebConstants.METHOD_GET);
    assertThat(Route.post(PATH, NOTHING).method()).isEqualTo(WebConstants.METHOD_POST);
    assertThat(Route.delete(PATH, NOTHING).method()).isEqualTo(WebConstants.METHOD_DELETE);
    assertThat(Route.patchUnder(PATH, NOTHING).method()).isEqualTo(WebConstants.METHOD_PATCH);
    assertThat(Route.deleteUnder(PATH, NOTHING).method()).isEqualTo(WebConstants.METHOD_DELETE);
  }

  @Test
  @DisplayName("a path with a regular expression character in it is matched literally")
  void aPathWithRegexCharactersIsMatchedLiterally() {
    Route route = Route.get(WITH_A_DOT, NOTHING);

    assertThat(route.match(WITH_A_DOT).matches()).isTrue();
    assertThat(route.match(DOT_AS_WILDCARD).matches())
        .as("the dot is a dot, not any character")
        .isFalse();
  }
}
