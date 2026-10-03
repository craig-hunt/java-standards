package com.sagecrest.standards.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.domain.errors.ValidationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class InventoryQueryTest {

  private static final String NO_SEARCH = "";
  private static final String PADDED_SEARCH = "  badge  ";
  private static final String TRIMMED_SEARCH = "badge";
  private static final String UPPERCASE_SEARCH = "BADGE";
  private static final String UNKNOWN = "colour";

  private static final String ALPHA = "Alpha";
  private static final String BRAVO = "Bravo";
  private static final String CHARLIE = "Charlie";

  private static final int FEW = 2;
  private static final int MANY = 9;
  private static final int SHARED_QUANTITY = 5;
  private static final int ONE_MATCH = 1;
  private static final int THREE_ROWS = 3;

  private static final List<InventoryItem> ROWS =
      List.of(
          new InventoryItem(CHARLIE, FEW, new Status(InventoryConstants.STATUS_LOW)),
          new InventoryItem(ALPHA, MANY, new Status(InventoryConstants.STATUS_OUT_OF_STOCK)),
          new InventoryItem(
              BRAVO, SHARED_QUANTITY, new Status(InventoryConstants.STATUS_IN_STOCK)));

  private static InventoryQuery sortedBy(String column, String direction) {
    return InventoryQuery.from(NO_SEARCH, column, direction);
  }

  private static List<String> namesFrom(InventoryQuery query) {
    return query.apply(ROWS).items().stream().map(InventoryItem::name).toList();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("orders by name ascending when the request names neither")
  void defaultsToNameAscending(String absent) {
    InventoryQuery query = InventoryQuery.from(absent, absent, absent);

    assertThat(query.sort().value()).isEqualTo(InventoryConstants.COLUMN_NAME);
    assertThat(query.direction().value()).isEqualTo(InventoryConstants.DIRECTION_ASCENDING);
    assertThat(query.search()).isEmpty();
  }

  @Test
  void rejectsAColumnTheTableCannotSortOn() {
    assertThatThrownBy(() -> sortedBy(UNKNOWN, null))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).code())
        .isEqualTo(InventoryConstants.CODE_INVALID_QUERY);
  }

  @Test
  void rejectsAnOrderThatDoesNotExist() {
    assertThatThrownBy(() -> sortedBy(null, UNKNOWN))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).getMessage())
        .isEqualTo(InventoryConstants.MSG_INVALID_DIRECTION);
  }

  @Test
  void trimsTheSearchTerm() {
    assertThat(InventoryQuery.from(PADDED_SEARCH, null, null).search()).isEqualTo(TRIMMED_SEARCH);
  }

  @Test
  @DisplayName("matches a name regardless of case")
  void matchesRegardlessOfCase() {
    InventoryResult matched =
        InventoryQuery.from(UPPERCASE_SEARCH, null, null).apply(InventorySeed.items());

    assertThat(matched.items())
        .extracting(InventoryItem::name)
        .containsExactly(InventoryConstants.SEED_ACCESS_BADGE);
    assertThat(matched.shown()).isEqualTo(ONE_MATCH);
    assertThat(matched.total()).isEqualTo(InventorySeed.items().size());
  }

  @Test
  void ordersByNameInBothDirections() {
    assertThat(namesFrom(sortedBy(InventoryConstants.COLUMN_NAME, null)))
        .containsExactly(ALPHA, BRAVO, CHARLIE);
    assertThat(
            namesFrom(
                sortedBy(InventoryConstants.COLUMN_NAME, InventoryConstants.DIRECTION_DESCENDING)))
        .containsExactly(CHARLIE, BRAVO, ALPHA);
  }

  @Test
  void ordersByQuantityInBothDirections() {
    assertThat(namesFrom(sortedBy(InventoryConstants.COLUMN_QUANTITY, null)))
        .containsExactly(CHARLIE, BRAVO, ALPHA);
    assertThat(
            namesFrom(
                sortedBy(
                    InventoryConstants.COLUMN_QUANTITY, InventoryConstants.DIRECTION_DESCENDING)))
        .containsExactly(ALPHA, BRAVO, CHARLIE);
  }

  @Test
  void ordersByStatusInBothDirections() {
    assertThat(namesFrom(sortedBy(InventoryConstants.COLUMN_STATUS, null)))
        .containsExactly(BRAVO, CHARLIE, ALPHA);
    assertThat(
            namesFrom(
                sortedBy(
                    InventoryConstants.COLUMN_STATUS, InventoryConstants.DIRECTION_DESCENDING)))
        .containsExactly(ALPHA, CHARLIE, BRAVO);
  }

  @Test
  @DisplayName("leaves rows that tie in the order the store returned them")
  void leavesTiesInTheOrderTheStoreReturned() {
    List<InventoryItem> tied =
        List.of(
            new InventoryItem(CHARLIE, SHARED_QUANTITY, new Status(InventoryConstants.STATUS_LOW)),
            new InventoryItem(ALPHA, SHARED_QUANTITY, new Status(InventoryConstants.STATUS_LOW)),
            new InventoryItem(BRAVO, SHARED_QUANTITY, new Status(InventoryConstants.STATUS_LOW)));

    InventoryResult ordered = sortedBy(InventoryConstants.COLUMN_QUANTITY, null).apply(tied);

    assertThat(ordered.items())
        .extracting(InventoryItem::name)
        .containsExactly(CHARLIE, ALPHA, BRAVO);
  }

  @Test
  @DisplayName("leaves the caller's list exactly as it was")
  void leavesTheCallersListAlone() {
    List<InventoryItem> caller = new ArrayList<>(ROWS);

    sortedBy(InventoryConstants.COLUMN_NAME, null).apply(caller);

    assertThat(caller).containsExactlyElementsOf(ROWS);
  }

  @Test
  void countsTheRowsItShowedAndTheRowsThatExist() {
    InventoryResult result = sortedBy(null, null).apply(ROWS);

    assertThat(result.shown()).isEqualTo(THREE_ROWS);
    assertThat(result.total()).isEqualTo(THREE_ROWS);
  }

  @Test
  void handsBackAnUnmodifiableList() {
    InventoryResult result = sortedBy(null, null).apply(ROWS);

    assertThatThrownBy(() -> result.items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
