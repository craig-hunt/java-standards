package com.sagecrest.standards.domain.errors;

import java.util.Map;

/**
 * The base every domain failure derives from, carrying a stable code and the per-field problems an
 * API can render.
 *
 * <p>Sealed, so the mapper at the edge switches over the whole hierarchy and the compiler proves it
 * handled every case. A new failure category cannot slip past the mapper and arrive at a client as
 * a bare 500: adding one to the permits clause breaks the switch until somebody decides what status
 * it deserves. That guarantee is the reason this hierarchy exists rather than one exception type
 * with a status field.
 *
 * <p>Unchecked on purpose. A checked exception asks every caller in between to either handle a
 * domain rule or redeclare it, and none of them can do anything useful with one: the only place
 * that knows what to do is the edge. Declaring these checked would spread {@code throws} up the
 * call chain and teach people to wrap them in try/catch blocks that rethrow, which is how a service
 * ends up catching its own validation errors and answering 200.
 *
 * <p>A code travels with the exception because callers match on identity, never on message text.
 */
public abstract sealed class DomainException extends RuntimeException
    permits ValidationException, NotFoundException {

  private final String code;
  private final Map<String, String> fields;

  DomainException(String code, String message, Map<String, String> fields) {
    super(message);
    this.code = code;
    this.fields = Map.copyOf(fields);
  }

  public String code() {
    return code;
  }

  /** The problems keyed by the field each one belongs to, empty when none apply. */
  public Map<String, String> fields() {
    return fields;
  }
}
