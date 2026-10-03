package com.sagecrest.standards.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.domain.errors.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InventoryErrorsTest {

  @Test
  @DisplayName("tells a bad column apart from a bad order while sharing one code")
  void tellsABadColumnApartFromABadOrder() {
    ValidationException badSort = InventoryErrors.invalidSort();
    ValidationException badDirection = InventoryErrors.invalidDirection();

    assertThat(badSort.code()).isEqualTo(InventoryConstants.CODE_INVALID_QUERY);
    assertThat(badSort).hasMessage(InventoryConstants.MSG_INVALID_SORT);
    assertThat(badSort.fields()).isEmpty();
    assertThat(badDirection.code()).isEqualTo(InventoryConstants.CODE_INVALID_QUERY);
    assertThat(badDirection).hasMessage(InventoryConstants.MSG_INVALID_DIRECTION);
    assertThat(badDirection.fields()).isEmpty();
  }
}
