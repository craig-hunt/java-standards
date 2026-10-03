package com.sagecrest.standards.domain.errors;

/**
 * The codes and messages every feature shares, as opposed to the per-feature codes that live beside
 * the feature they describe.
 *
 * <p>A validation failure answers with one envelope code and puts the per-field sentences in the
 * fields map, so a client matches on a stable code while a form renders the sentence next to the
 * input that caused it.
 */
public final class ErrorConstants {

  public static final String CODE_INVALID_BODY = "invalid_body";
  public static final String CODE_VALIDATION = "validation_failed";
  public static final String CODE_INTERNAL = "internal_error";

  public static final String MSG_INVALID_BODY =
      "request body must hold one JSON object with known fields";
  public static final String MSG_VALIDATION = "one or more fields need attention";
  public static final String MSG_INTERNAL = "the server could not complete the request";

  private ErrorConstants() {}
}
