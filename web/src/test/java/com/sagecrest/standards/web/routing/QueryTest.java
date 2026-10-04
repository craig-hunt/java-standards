package com.sagecrest.standards.web.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.web.WebConstants;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QueryTest {

  private static final String SEARCH = "badge";
  private static final String SORT_COLUMN = "name";
  private static final String WITH_AMPERSAND = "ampersand & more";
  private static final String WITH_EQUALS = "a=b";
  private static final String DECODED_SPACE = "badge case";

  private static final String NO_QUERY = "/api/inventory";
  private static final String BOTH_PARAMETERS = "/api/inventory?search=badge&sort=name";
  private static final String ONLY_SORT = "/api/inventory?sort=name";
  private static final String BARE_NAME = "/api/inventory?search";
  private static final String EMPTY_VALUE = "/api/inventory?search=";
  private static final String ENCODED_SPACE = "/api/inventory?search=badge%20case";
  private static final String ENCODED_AMPERSAND = "/api/inventory?search=ampersand%20%26%20more";
  private static final String ENCODED_EQUALS = "/api/inventory?search=a%3Db";
  private static final String UNENCODED_EQUALS = "/api/inventory?search=a=b";

  private static Query of(String path) {
    return Query.of(new FakeExchange(WebConstants.METHOD_GET, path));
  }

  @Test
  @DisplayName("the canonical constructor copies too, not only the factory")
  void theCanonicalConstructorCopies() {
    Map<String, String> mutable = new HashMap<>();
    mutable.put(WebConstants.QUERY_SEARCH, SEARCH);
    Query query = new Query(mutable);

    mutable.put(WebConstants.QUERY_SORT, SORT_COLUMN);

    assertThat(query.get(WebConstants.QUERY_SORT))
        .as("a request already parsed cannot gain a parameter afterwards")
        .isNull();
    assertThatThrownBy(() -> query.values().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void readsEachParameter() {
    Query query = of(BOTH_PARAMETERS);

    assertThat(query.get(WebConstants.QUERY_SEARCH)).isEqualTo(SEARCH);
    assertThat(query.get(WebConstants.QUERY_SORT)).isEqualTo(SORT_COLUMN);
  }

  @Test
  @DisplayName("an absent parameter reads as absent, which is what the domain treats as a default")
  void anAbsentParameterReadsAsAbsent() {
    assertThat(of(NO_QUERY).get(WebConstants.QUERY_SEARCH)).isNull();
    assertThat(of(ONLY_SORT).get(WebConstants.QUERY_SEARCH)).isNull();
  }

  @Test
  @DisplayName("a parameter with no value reads as empty rather than absent")
  void aValuelessParameterReadsAsEmpty() {
    assertThat(of(BARE_NAME).get(WebConstants.QUERY_SEARCH)).isEmpty();
    assertThat(of(EMPTY_VALUE).get(WebConstants.QUERY_SEARCH)).isEmpty();
  }

  @Test
  void decodesPercentEscapes() {
    assertThat(of(ENCODED_SPACE).get(WebConstants.QUERY_SEARCH)).isEqualTo(DECODED_SPACE);
  }

  @Test
  @DisplayName("an encoded separator stays inside its value instead of splitting the query")
  void anEncodedSeparatorStaysInsideItsValue() {
    assertThat(of(ENCODED_AMPERSAND).get(WebConstants.QUERY_SEARCH)).isEqualTo(WITH_AMPERSAND);
    assertThat(of(ENCODED_EQUALS).get(WebConstants.QUERY_SEARCH)).isEqualTo(WITH_EQUALS);
  }

  @Test
  @DisplayName("an unencoded equals inside a value is kept, because only the first one separates")
  void anUnencodedEqualsInsideAValueIsKept() {
    assertThat(of(UNENCODED_EQUALS).get(WebConstants.QUERY_SEARCH)).isEqualTo(WITH_EQUALS);
  }
}
