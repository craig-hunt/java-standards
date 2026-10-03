package com.sagecrest.standards.domain.errors;

import java.util.Map;

/**
 * Raised when a request carries a value the domain rejects.
 *
 * <p>One type rather than one type per failure. The C# sibling gives every failure its own class,
 * which the edge there needs because it switches on the type to pick a status. Here the sealed base
 * already splits the two categories a status depends on, so a separate class per failure would add
 * names without adding a guarantee. The named factories beside each feature supply the readable
 * construction site instead, and tests assert on the code a client actually matches rather than on
 * a class name no client can see.
 */
public final class ValidationException extends DomainException {

  public ValidationException(String code, String message) {
    super(code, message, Map.of());
  }

  public ValidationException(String code, String message, Map<String, String> fields) {
    super(code, message, fields);
  }
}
