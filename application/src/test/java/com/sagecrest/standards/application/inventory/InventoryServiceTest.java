package com.sagecrest.standards.application.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.application.ports.InventoryStore;
import com.sagecrest.standards.domain.inventory.InventoryConstants;
import com.sagecrest.standards.domain.inventory.InventoryItem;
import com.sagecrest.standards.domain.inventory.InventoryQuery;
import com.sagecrest.standards.domain.inventory.InventoryResult;
import com.sagecrest.standards.domain.inventory.InventorySeed;
import com.sagecrest.standards.domain.inventory.Status;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {

  private static final String SEARCH = "badge";
  private static final String ALPHA = "Alpha";
  private static final String BRAVO = "Bravo";
  private static final int FEW = 2;
  private static final int MANY = 9;
  private static final int ONE_MATCH = 1;

  private static final List<InventoryItem> ROWS =
      List.of(
          new InventoryItem(BRAVO, MANY, new Status(InventoryConstants.STATUS_IN_STOCK)),
          new InventoryItem(ALPHA, FEW, new Status(InventoryConstants.STATUS_LOW)));

  @Test
  @DisplayName("orders the store rows rather than trusting the order they arrived in")
  void ordersTheStoreRows() {
    InventoryResult result =
        new InventoryService(() -> ROWS).query(InventoryQuery.from(null, null, null));

    assertThat(result.items()).extracting(InventoryItem::name).containsExactly(ALPHA, BRAVO);
    assertThat(result.total()).isEqualTo(ROWS.size());
  }

  @Test
  void narrowsToTheRowsTheSearchMatches() {
    InventoryStore store = InventorySeed::items;

    InventoryResult result =
        new InventoryService(store).query(InventoryQuery.from(SEARCH, null, null));

    assertThat(result.shown()).isEqualTo(ONE_MATCH);
    assertThat(result.total()).isEqualTo(InventorySeed.items().size());
  }
}
