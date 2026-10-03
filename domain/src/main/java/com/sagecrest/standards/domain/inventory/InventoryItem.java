package com.sagecrest.standards.domain.inventory;

/** One row of the stock table. */
public record InventoryItem(String name, int quantity, Status status) {}
