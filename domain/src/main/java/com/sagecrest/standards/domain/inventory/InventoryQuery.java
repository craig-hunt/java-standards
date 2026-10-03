package com.sagecrest.standards.domain.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A validated stock query: what to search for, which column orders the rows, and which way that
 * order runs.
 *
 * <p>The Go sibling parses this straight from the query string. The factory here takes three values
 * instead, so the domain states the rule without knowing that a query string exists or what its
 * keys are called.
 */
public record InventoryQuery(String search, Column sort, Direction direction) {

  private static final String ABSENT = "";

  public static InventoryQuery from(String search, String sort, String direction) {
    Column column = new Column(sort);
    if (column.isAbsent()) {
      column = new Column(InventoryConstants.COLUMN_NAME);
    } else if (!column.known()) {
      throw InventoryErrors.invalidSort();
    }

    Direction order = new Direction(direction);
    if (order.isAbsent()) {
      order = new Direction(InventoryConstants.DIRECTION_ASCENDING);
    } else if (!order.known()) {
      throw InventoryErrors.invalidDirection();
    }

    return new InventoryQuery(Objects.requireNonNullElse(search, ABSENT).trim(), column, order);
  }

  /**
   * Filters and orders a set of rows, leaving the caller's list untouched.
   *
   * <p>The sort is stable, so rows that tie on the sort column keep the order the store returned
   * rather than shuffling between requests.
   */
  public InventoryResult apply(List<InventoryItem> items) {
    String needle = search.toLowerCase(Locale.ROOT);
    List<InventoryItem> matched = new ArrayList<>(items.size());
    for (InventoryItem item : items) {
      if (item.name().toLowerCase(Locale.ROOT).contains(needle)) {
        matched.add(item);
      }
    }
    matched.sort(ordering());
    return new InventoryResult(matched, matched.size(), items.size());
  }

  private Comparator<InventoryItem> ordering() {
    Comparator<InventoryItem> byColumn =
        switch (sort.value()) {
          case InventoryConstants.COLUMN_QUANTITY ->
              Comparator.comparingInt(InventoryItem::quantity);
          case InventoryConstants.COLUMN_STATUS ->
              Comparator.comparing((InventoryItem item) -> item.status().value());
          default -> Comparator.comparing(InventoryItem::name);
        };
    return direction.descending() ? byColumn.reversed() : byColumn;
  }
}
