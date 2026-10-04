package com.sagecrest.standards.web.contracts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts that the response records hold their lists, rather than borrowing them.
 *
 * <p>Built directly rather than through {@code from}, because the factories were already copying
 * and the canonical constructors were not. A record that keeps its invariant on only one of two
 * paths in keeps it by convention, and a convention is what this repository exists to replace.
 */
class ContractsTest {

  private static final String NAME = "Access badge";
  private static final String OTHER_NAME = "Docking station";
  private static final String STATUS = "In stock";
  private static final int QUANTITY = 240;
  private static final int ONE_ROW = 1;
  private static final long ID = 1L;
  private static final String TITLE = "Read the ADR";
  private static final boolean NOT_COMPLETED = false;

  @Test
  @DisplayName("a stock response cannot gain a row after it was built")
  void aStockResponseCannotGainARow() {
    List<Contracts.InventoryItemResponse> mutable = new ArrayList<>();
    mutable.add(new Contracts.InventoryItemResponse(NAME, QUANTITY, STATUS));
    Contracts.InventoryResponse response =
        new Contracts.InventoryResponse(mutable, ONE_ROW, ONE_ROW);

    mutable.add(new Contracts.InventoryItemResponse(OTHER_NAME, QUANTITY, STATUS));

    assertThat(response.items()).hasSize(ONE_ROW);
    assertThatThrownBy(() -> response.items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("a task list cannot gain a task after it was built")
  void aTaskListCannotGainATask() {
    List<Contracts.TaskResponse> mutable = new ArrayList<>();
    mutable.add(new Contracts.TaskResponse(ID, TITLE, NOT_COMPLETED));
    Contracts.TaskViewResponse response = new Contracts.TaskViewResponse(mutable, ONE_ROW, ONE_ROW);

    mutable.add(new Contracts.TaskResponse(ID, TITLE, NOT_COMPLETED));

    assertThat(response.tasks()).hasSize(ONE_ROW);
    assertThatThrownBy(() -> response.tasks().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
