package com.sagecrest.standards.infrastructure.persistence;

/**
 * A failure that belongs to a dependency rather than to a rule.
 *
 * <p>Deliberately outside the sealed {@code DomainException} hierarchy. The edge maps every domain
 * failure to a status a client can act on; this one means the service could not do its job, which
 * is a different answer and a different log level. Keeping the two hierarchies apart is what lets
 * the mapper stay exhaustive over the first without swallowing the second.
 */
public final class InfrastructureException extends RuntimeException {

  public InfrastructureException(String message, Throwable cause) {
    super(message, cause);
  }

  public InfrastructureException(String message) {
    super(message);
  }
}
