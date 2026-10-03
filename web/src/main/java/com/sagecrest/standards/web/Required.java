package com.sagecrest.standards.web;

import java.util.function.UnaryOperator;

/**
 * Reads a setting that has no sensible default.
 *
 * <p>A blank value counts as absent. An empty string in a deployment file is somebody's
 * placeholder, and accepting one as a password or a token means starting a service that cannot work
 * or, worse, one whose token check compares against nothing.
 */
final class Required {

  static String read(UnaryOperator<String> environment, String name) {
    String value = environment.apply(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(WebConstants.MSG_MISSING_SETTING.formatted(name));
    }
    return value;
  }

  private Required() {}
}
