package com.sagecrest.standards.web.errors;

import com.sagecrest.standards.domain.errors.ErrorConstants;

/**
 * Raised when a request body is absent, oversized, or not the JSON object the route expects.
 *
 * <p>It lives in the web module rather than in the domain, because a malformed body is not a rule
 * the domain holds an opinion about: the domain never sees one. Keeping it out also keeps the
 * sealed {@code DomainException} hierarchy closed, so the mapper's switch over domain failures
 * stays exhaustive.
 */
public final class InvalidRequestBodyException extends RuntimeException {

  public InvalidRequestBodyException(Throwable cause) {
    super(ErrorConstants.MSG_INVALID_BODY, cause);
  }

  public InvalidRequestBodyException() {
    super(ErrorConstants.MSG_INVALID_BODY);
  }
}
