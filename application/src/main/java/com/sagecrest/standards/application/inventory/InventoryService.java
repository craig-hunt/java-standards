package com.sagecrest.standards.application.inventory;

import com.sagecrest.standards.application.ports.InventoryStore;
import com.sagecrest.standards.domain.inventory.InventoryQuery;
import com.sagecrest.standards.domain.inventory.InventoryResult;

/** Answers a stock query. */
public final class InventoryService {

  private final InventoryStore store;

  public InventoryService(InventoryStore store) {
    this.store = store;
  }

  public InventoryResult query(InventoryQuery query) {
    return query.apply(store.items());
  }
}
