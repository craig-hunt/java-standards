package com.sagecrest.standards.domain.inventory;

/** Every literal the inventory feature carries, named once. */
public final class InventoryConstants {

  public static final String COLUMN_NAME = "name";
  public static final String COLUMN_QUANTITY = "quantity";
  public static final String COLUMN_STATUS = "status";

  public static final String DIRECTION_ASCENDING = "ascending";
  public static final String DIRECTION_DESCENDING = "descending";

  public static final String STATUS_IN_STOCK = "In stock";
  public static final String STATUS_LOW = "Low";
  public static final String STATUS_OUT_OF_STOCK = "Out of stock";

  public static final String CODE_INVALID_QUERY = "invalid_query";
  public static final String MSG_INVALID_SORT = "sort must be name, quantity, or status";
  public static final String MSG_INVALID_DIRECTION = "direction must be ascending or descending";

  public static final String SEED_ACCESS_BADGE = "Access badge";
  public static final String SEED_DOCKING_STATION = "Docking station";
  public static final String SEED_LAPTOP_SLEEVE = "Laptop sleeve";
  public static final String SEED_MONITOR_ARM = "Monitor arm";
  public static final String SEED_HEADSET = "Noise-cancelling headset";
  public static final String SEED_WEBCAM = "Webcam";

  public static final int SEED_ACCESS_BADGE_QUANTITY = 240;
  public static final int SEED_DOCKING_STATION_QUANTITY = 12;
  public static final int SEED_LAPTOP_SLEEVE_QUANTITY = 0;
  public static final int SEED_MONITOR_ARM_QUANTITY = 58;
  public static final int SEED_HEADSET_QUANTITY = 4;
  public static final int SEED_WEBCAM_QUANTITY = 31;

  private InventoryConstants() {}
}
