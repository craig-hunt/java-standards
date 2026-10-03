package com.sagecrest.standards.domain.inventory;

import java.util.List;

/** What a stock query answers: the rows that matched, how many matched, and how many rows exist. */
public record InventoryResult(List<InventoryItem> items, int shown, int total) {

  public InventoryResult {
    items = List.copyOf(items);
  }
}
