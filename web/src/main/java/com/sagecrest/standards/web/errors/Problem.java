package com.sagecrest.standards.web.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * One failure, in the shape RFC 9457 defines.
 *
 * <p>{@code code} and {@code fields} are the extension members. The Go sibling answers with a bare
 * code, message and fields object; this adds the envelope and media type the standard defines while
 * keeping the machine-readable parts a client already matches on. That divergence is deliberate.
 *
 * <p>{@code fields} is omitted when empty rather than written as {@code {}}, so a client can treat
 * its presence as meaning there are per-field problems to render.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problem(
    String type, String title, int status, String detail, String code, Map<String, String> fields) {

  private static final String TYPE_BLANK = "about:blank";

  public Problem {
    // Absent and empty mean the same thing here, and both have to produce the
    // same wire shape. A client distinguishes a failure carrying per-field
    // problems from one without by whether the member is present, so writing
    // an empty object would say there are field problems and then list none.
    // The factory normalized this and the canonical constructor did not, which
    // left the record's stated contract true on one of two paths in.
    //
    // Anything present is copied, so the map a caller still holds cannot change
    // what was sent.
    fields = fields == null || fields.isEmpty() ? null : Map.copyOf(fields);
  }

  public static Problem of(int status, String title, String code, String detail) {
    return new Problem(TYPE_BLANK, title, status, detail, code, null);
  }

  public static Problem of(
      int status, String title, String code, String detail, Map<String, String> fields) {
    return new Problem(
        TYPE_BLANK, title, status, detail, code, fields.isEmpty() ? null : Map.copyOf(fields));
  }
}
