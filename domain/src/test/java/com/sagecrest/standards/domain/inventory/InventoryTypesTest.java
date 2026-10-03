package com.sagecrest.standards.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InventoryTypesTest {

  private static final String UNKNOWN = "colour";
  private static final int SEED_ROWS = 6;

  @ParameterizedTest
  @NullAndEmptySource
  void readsAnAbsentColumnAsAbsent(String raw) {
    assertThat(new Column(raw).isAbsent()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        InventoryConstants.COLUMN_NAME,
        InventoryConstants.COLUMN_QUANTITY,
        InventoryConstants.COLUMN_STATUS
      })
  void recognizesEveryColumnTheTableSortsOn(String raw) {
    assertThat(new Column(raw).known()).isTrue();
  }

  @Test
  void refusesAColumnItDoesNotRecognize() {
    assertThat(new Column(UNKNOWN).known()).isFalse();
    assertThat(new Column(UNKNOWN).isAbsent()).isFalse();
    assertThat(new Column(UNKNOWN)).hasToString(UNKNOWN);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void readsAnAbsentDirectionAsAbsent(String raw) {
    assertThat(new Direction(raw).isAbsent()).isTrue();
  }

  @Test
  @DisplayName("reports descending for one order and not the other")
  void reportsDescendingForOneOrderOnly() {
    Direction descending = new Direction(InventoryConstants.DIRECTION_DESCENDING);
    Direction ascending = new Direction(InventoryConstants.DIRECTION_ASCENDING);

    assertThat(descending.descending()).isTrue();
    assertThat(descending.known()).isTrue();
    assertThat(ascending.descending()).isFalse();
    assertThat(ascending.known()).isTrue();
    assertThat(descending).hasToString(InventoryConstants.DIRECTION_DESCENDING);
    assertThat(ascending).hasToString(InventoryConstants.DIRECTION_ASCENDING);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void readsAnAbsentStatusAsEmpty(String raw) {
    assertThat(new Status(raw).value()).isEmpty();
  }

  @Test
  void keepsTheWordsTheTableShows() {
    assertThat(new Status(InventoryConstants.STATUS_LOW))
        .hasToString(InventoryConstants.STATUS_LOW);
  }

  @Test
  @DisplayName("seeds a recognizable table that nobody can change afterwards")
  void seedsAnUnmodifiableTable() {
    assertThat(InventorySeed.items()).hasSize(SEED_ROWS);
    assertThatThrownBy(() -> InventorySeed.items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
