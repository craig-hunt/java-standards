package com.sagecrest.standards.web.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.errors.InvalidRequestBodyException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The one JSON configuration this surface uses, and the only place a request body is read.
 *
 * <p>Unknown members fail rather than being ignored. A client that misspells {@code completed}
 * would otherwise get a 200 and no change, and would have no way to discover the typo.
 *
 * <p>The read is capped. {@code readAllBytes} on a request body is an invitation to send a stream
 * that never ends, and the server would hold memory until it died rather than answering 400.
 *
 * <p>Trailing content fails too. Jackson reads the first value and stops, so a body of {@code
 * {"title":"a"}{"title":"b"}} would bind the first object and discard the second without a word.
 * The route's contract says one JSON object, and a client sending two has misunderstood something
 * it should be told about.
 */
public final class Json {

  private static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();

  private static final int END_OF_STREAM = -1;

  public static byte[] write(Object value) {
    try {
      return MAPPER.writeValueAsBytes(value);
    } catch (IOException unwritable) {
      // A response built from records this module controls has no unwritable shape; a
      // checked exception here would only force a catch nobody can act on.
      throw new IllegalStateException(unwritable);
    }
  }

  public static <T> T read(InputStream body, Class<T> shape) {
    T bound;
    try {
      bound = MAPPER.readValue(bounded(body), shape);
    } catch (IOException unreadable) {
      throw new InvalidRequestBodyException(unreadable);
    }
    if (bound == null) {
      // A body of the four characters n-u-l-l is valid JSON and binds to null without
      // complaint. Returning it would hand every endpoint a reference to dereference,
      // and the mapper would answer 500 for what is plainly a malformed request.
      throw new InvalidRequestBodyException();
    }
    return bound;
  }

  private static String bounded(InputStream body) throws IOException {
    byte[] bytes = body.readNBytes(WebConstants.MAX_BODY_BYTES);
    if (body.read() != END_OF_STREAM) {
      throw new InvalidRequestBodyException();
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private Json() {}
}
