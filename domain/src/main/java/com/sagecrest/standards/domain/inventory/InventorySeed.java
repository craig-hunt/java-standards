package com.sagecrest.standards.domain.inventory;

import java.util.List;

/**
 * The rows a fresh database starts with, so the reference API answers with something recognizable
 * before anyone adds stock.
 */
public final class InventorySeed {

  private static final List<InventoryItem> ITEMS =
      List.of(
          new InventoryItem(
              InventoryConstants.SEED_ACCESS_BADGE,
              InventoryConstants.SEED_ACCESS_BADGE_QUANTITY,
              new Status(InventoryConstants.STATUS_IN_STOCK)),
          new InventoryItem(
              InventoryConstants.SEED_DOCKING_STATION,
              InventoryConstants.SEED_DOCKING_STATION_QUANTITY,
              new Status(InventoryConstants.STATUS_LOW)),
          new InventoryItem(
              InventoryConstants.SEED_LAPTOP_SLEEVE,
              InventoryConstants.SEED_LAPTOP_SLEEVE_QUANTITY,
              new Status(InventoryConstants.STATUS_OUT_OF_STOCK)),
          new InventoryItem(
              InventoryConstants.SEED_MONITOR_ARM,
              InventoryConstants.SEED_MONITOR_ARM_QUANTITY,
              new Status(InventoryConstants.STATUS_IN_STOCK)),
          new InventoryItem(
              InventoryConstants.SEED_HEADSET,
              InventoryConstants.SEED_HEADSET_QUANTITY,
              new Status(InventoryConstants.STATUS_LOW)),
          new InventoryItem(
              InventoryConstants.SEED_WEBCAM,
              InventoryConstants.SEED_WEBCAM_QUANTITY,
              new Status(InventoryConstants.STATUS_IN_STOCK)));

  public static List<InventoryItem> items() {
    return ITEMS;
  }

  private InventorySeed() {}
}
