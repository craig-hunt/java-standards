package com.sagecrest.standards.domain.inventory;

import java.util.Objects;
import java.util.Set;

/** Which way a sort runs. */
public record Direction(String value) {

  private static final String ABSENT = "";

  private static final Set<String> KNOWN =
      Set.of(InventoryConstants.DIRECTION_ASCENDING, InventoryConstants.DIRECTION_DESCENDING);

  public Direction {
    value = Objects.requireNonNullElse(value, ABSENT);
  }

  public boolean isAbsent() {
    return value.isEmpty();
  }

  public boolean known() {
    return KNOWN.contains(value);
  }

  public boolean descending() {
    return InventoryConstants.DIRECTION_DESCENDING.equals(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
