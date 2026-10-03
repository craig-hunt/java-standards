package com.sagecrest.standards.web;

import java.util.function.UnaryOperator;

/**
 * Everything the API reads from its environment, checked once at startup.
 *
 * <p>Checked here rather than where each value is used. A missing database password discovered on
 * the first request is an outage; discovered at startup it is a deployment that never went live.
 * The siblings validate the same way and for the same reason.
 *
 * <p>The lookup arrives as a function so a test can supply an environment without setting process
 * variables, which no test can do in Java and which would leak between tests if it could.
 */
public record Settings(int port, DatabaseSettings database, String apiToken) {

  private static final int DEFAULT_PORT = 8080;

  public static Settings fromEnvironment() {
    return from(System::getenv);
  }

  public static Settings from(UnaryOperator<String> environment) {
    String token = Required.read(environment, WebConstants.ENV_API_TOKEN);
    if (token.indexOf(WebConstants.JWT_SEPARATOR) >= 0) {
      throw new IllegalStateException(WebConstants.MSG_TOKEN_HAS_SEPARATOR);
    }
    return new Settings(port(environment), DatabaseSettings.from(environment), token);
  }

  private static int port(UnaryOperator<String> environment) {
    String configured = environment.apply(WebConstants.ENV_PORT);
    if (configured == null || configured.isBlank()) {
      return DEFAULT_PORT;
    }
    try {
      return Integer.parseInt(configured.trim());
    } catch (NumberFormatException notANumber) {
      throw new IllegalStateException(WebConstants.MSG_PORT_NOT_A_NUMBER, notANumber);
    }
  }
}
