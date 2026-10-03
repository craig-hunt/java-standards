package com.sagecrest.standards.domain.inventory;

import com.sagecrest.standards.domain.errors.ValidationException;

/** The failures the inventory feature raises. */
public final class InventoryErrors {

  public static ValidationException invalidSort() {
    return new ValidationException(
        InventoryConstants.CODE_INVALID_QUERY, InventoryConstants.MSG_INVALID_SORT);
  }

  public static ValidationException invalidDirection() {
    return new ValidationException(
        InventoryConstants.CODE_INVALID_QUERY, InventoryConstants.MSG_INVALID_DIRECTION);
  }

  private InventoryErrors() {}
}
