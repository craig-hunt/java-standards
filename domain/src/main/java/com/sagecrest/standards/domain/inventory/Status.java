package com.sagecrest.standards.domain.inventory;

import java.util.Objects;

/** The stock status a row reports, in the words the table shows. */
public record Status(String value) {

  private static final String ABSENT = "";

  public Status {
    value = Objects.requireNonNullElse(value, ABSENT);
  }

  @Override
  public String toString() {
    return value;
  }
}
