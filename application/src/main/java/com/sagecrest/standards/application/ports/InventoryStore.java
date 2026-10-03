package com.sagecrest.standards.application.ports;

import com.sagecrest.standards.domain.inventory.InventoryItem;
import java.util.List;

/**
 * Reads the stock rows. Searching and ordering stay in the domain, so a second store implementation
 * cannot quietly answer with a different order.
 */
public interface InventoryStore {

  List<InventoryItem> items();
}
