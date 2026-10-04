package com.sagecrest.standards.web.errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.json.Json;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts the record's contract through its canonical constructor, not only through its factory.
 *
 * <p>The factory was normalizing an empty map to absent and the constructor was not, so the stated
 * contract held on one of the two ways in. A test that only ever called the factory could not see
 * that, which is why these construct directly.
 */
class ProblemTest {

  private static final String CODE = "a_code";
  private static final String DETAIL = "a detail";
  private static final String FIELD = "title";
  private static final String PROBLEM = "Enter a title.";
  private static final String OTHER_FIELD = "completed";
  private static final String FIELDS_MEMBER = "fields";
  private static final String TYPE_BLANK = "about:blank";

  private static String serialized(Problem problem) {
    return new String(Json.write(problem), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("an empty map reads as absent, however the record was built")
  void anEmptyMapReadsAsAbsent() {
    Problem viaFactory =
        Problem.of(
            WebConstants.STATUS_UNPROCESSABLE,
            WebConstants.TITLE_UNPROCESSABLE,
            CODE,
            DETAIL,
            Map.of());
    Problem viaConstructor =
        new Problem(
            TYPE_BLANK,
            WebConstants.TITLE_UNPROCESSABLE,
            WebConstants.STATUS_UNPROCESSABLE,
            DETAIL,
            CODE,
            Map.of());

    assertThat(viaFactory.fields()).isNull();
    assertThat(viaConstructor.fields())
        .as("writing an empty object would claim field problems and then list none")
        .isNull();
    assertThat(serialized(viaConstructor)).doesNotContain(FIELDS_MEMBER);
  }

  @Test
  void anAbsentMapStaysAbsent() {
    Problem problem =
        new Problem(
            TYPE_BLANK,
            WebConstants.TITLE_NOT_FOUND,
            WebConstants.STATUS_NOT_FOUND,
            DETAIL,
            CODE,
            null);

    assertThat(problem.fields()).isNull();
    assertThat(serialized(problem)).doesNotContain(FIELDS_MEMBER);
  }

  @Test
  @DisplayName("a populated map is rendered, so a form can place each message")
  void aPopulatedMapIsRendered() {
    Problem problem =
        Problem.of(
            WebConstants.STATUS_UNPROCESSABLE,
            WebConstants.TITLE_UNPROCESSABLE,
            CODE,
            DETAIL,
            Map.of(FIELD, PROBLEM));

    assertThat(problem.fields()).containsExactly(Map.entry(FIELD, PROBLEM));
    assertThat(serialized(problem)).contains(FIELDS_MEMBER, FIELD, PROBLEM);
  }

  @Test
  @DisplayName("the map is copied, so a caller cannot change what was already answered")
  void theMapIsCopied() {
    Map<String, String> mutable = new HashMap<>();
    mutable.put(FIELD, PROBLEM);
    Problem problem =
        new Problem(
            TYPE_BLANK,
            WebConstants.TITLE_UNPROCESSABLE,
            WebConstants.STATUS_UNPROCESSABLE,
            DETAIL,
            CODE,
            mutable);

    mutable.put(OTHER_FIELD, PROBLEM);

    assertThat(problem.fields()).containsExactly(Map.entry(FIELD, PROBLEM));
    assertThatThrownBy(() -> problem.fields().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
