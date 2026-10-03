package com.sagecrest.standards.domain.errors;

import java.util.Map;

/** Raised when a request names a resource that does not exist. */
public final class NotFoundException extends DomainException {

  public NotFoundException(String code, String message) {
    super(code, message, Map.of());
  }
}
