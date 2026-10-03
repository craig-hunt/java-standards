package com.sagecrest.standards.web;

import java.util.function.UnaryOperator;

/**
 * Where the database is, and nothing else.
 *
 * <p>Separate from {@link Settings} because the migrator needs exactly this and nothing else. A
 * migrator that read the whole configuration would refuse to run without an API token it never
 * presents, and somebody would eventually set a placeholder token to get a migration through.
 */
public record DatabaseSettings(String url, String user, String password) {

  public static DatabaseSettings fromEnvironment() {
    return from(System::getenv);
  }

  public static DatabaseSettings from(UnaryOperator<String> environment) {
    return new DatabaseSettings(
        Required.read(environment, WebConstants.ENV_DATABASE_URL),
        Required.read(environment, WebConstants.ENV_DATABASE_USER),
        Required.read(environment, WebConstants.ENV_DATABASE_PASSWORD));
  }
}
