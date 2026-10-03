package com.sagecrest.standards.infrastructure.stores;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.domain.inventory.InventoryConstants;
import com.sagecrest.standards.domain.inventory.InventoryItem;
import com.sagecrest.standards.domain.inventory.InventorySeed;
import com.sagecrest.standards.infrastructure.persistence.PostgresFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JdbcInventoryStoreIT extends PostgresFixture {

  private final JdbcInventoryStore store = new JdbcInventoryStore(database());

  @Test
  @DisplayName("reads back the seed the schema applied, from the domain's own list")
  void readsBackTheSeed() {
    assertThat(store.items()).containsExactlyInAnyOrderElementsOf(InventorySeed.items());
  }

  @Test
  @DisplayName("seeds once, so a second startup does not duplicate a row")
  void seedsOnce() {
    assertThat(store.items()).hasSameSizeAs(InventorySeed.items());
  }

  @Test
  void orderingByNameMakesTheInputToTheDomainSortStable() {
    assertThat(store.items())
        .extracting(InventoryItem::name)
        .startsWith(InventoryConstants.SEED_ACCESS_BADGE);
  }

  @Test
  void keepsTheStatusWordsTheTableShows() {
    assertThat(store.items())
        .filteredOn(item -> item.name().equals(InventoryConstants.SEED_LAPTOP_SLEEVE))
        .singleElement()
        .extracting(item -> item.status().value())
        .isEqualTo(InventoryConstants.STATUS_OUT_OF_STOCK);
  }
}
