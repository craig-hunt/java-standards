package com.sagecrest.standards.domain.inventory;

import java.util.Objects;
import java.util.Set;

/** A column the stock table sorts on. */
public record Column(String value) {

  private static final String ABSENT = "";

  private static final Set<String> KNOWN =
      Set.of(
          InventoryConstants.COLUMN_NAME,
          InventoryConstants.COLUMN_QUANTITY,
          InventoryConstants.COLUMN_STATUS);

  public Column {
    value = Objects.requireNonNullElse(value, ABSENT);
  }

  public boolean isAbsent() {
    return value.isEmpty();
  }

  public boolean known() {
    return KNOWN.contains(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
