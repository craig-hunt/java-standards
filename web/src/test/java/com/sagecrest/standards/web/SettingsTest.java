package com.sagecrest.standards.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SettingsTest {

  private static final String URL = "jdbc:postgresql://localhost:5432/standards";
  private static final String USER = "standards";
  private static final String PASSWORD = "a-password";
  private static final String TOKEN = "a-token";
  private static final String TOKEN_WITH_PERIOD = "header.payload.signature";
  private static final String NOT_A_NUMBER = "eight thousand";
  private static final String CONFIGURED_PORT = "9090";
  private static final String PADDED_PORT = "  9090  ";
  private static final int EXPECTED_PORT = 9090;
  private static final int DEFAULT_PORT = 8080;

  private static Map<String, String> complete() {
    Map<String, String> environment = new HashMap<>();
    environment.put(WebConstants.ENV_DATABASE_URL, URL);
    environment.put(WebConstants.ENV_DATABASE_USER, USER);
    environment.put(WebConstants.ENV_DATABASE_PASSWORD, PASSWORD);
    environment.put(WebConstants.ENV_API_TOKEN, TOKEN);
    return environment;
  }

  @Test
  void readsACompleteEnvironment() {
    Settings settings = Settings.from(complete()::get);

    assertThat(settings.database().url()).isEqualTo(URL);
    assertThat(settings.database().user()).isEqualTo(USER);
    assertThat(settings.database().password()).isEqualTo(PASSWORD);
    assertThat(settings.apiToken()).isEqualTo(TOKEN);
  }

  @Test
  @DisplayName("defaults the port, because a port has a sensible default and a password does not")
  void defaultsThePort() {
    assertThat(Settings.from(complete()::get).port()).isEqualTo(DEFAULT_PORT);
  }

  @Test
  void readsAConfiguredPort() {
    Map<String, String> environment = complete();
    environment.put(WebConstants.ENV_PORT, CONFIGURED_PORT);

    assertThat(Settings.from(environment::get).port()).isEqualTo(EXPECTED_PORT);
  }

  @Test
  @DisplayName("tolerates whitespace around a port, which a deployment file tends to add")
  void toleratesWhitespaceAroundAPort() {
    Map<String, String> environment = complete();
    environment.put(WebConstants.ENV_PORT, PADDED_PORT);

    assertThat(Settings.from(environment::get).port()).isEqualTo(EXPECTED_PORT);
  }

  @Test
  void refusesAPortThatIsNotANumber() {
    Map<String, String> environment = complete();
    environment.put(WebConstants.ENV_PORT, NOT_A_NUMBER);

    assertThatThrownBy(() -> Settings.from(environment::get))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(WebConstants.MSG_PORT_NOT_A_NUMBER);
  }

  @ParameterizedTest
  @DisplayName("names the variable it needs rather than reporting that something is missing")
  @ValueSource(strings = {"DATABASE_URL", "DATABASE_USER", "DATABASE_PASSWORD", "API_TOKEN"})
  void namesTheMissingVariable(String missing) {
    Map<String, String> environment = complete();
    environment.remove(missing);

    assertThatThrownBy(() -> Settings.from(environment::get))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(WebConstants.MSG_MISSING_SETTING.formatted(missing));
  }

  @ParameterizedTest
  @DisplayName("treats a blank value as absent, because a blank password is not a password")
  @ValueSource(strings = {"", "   "})
  void treatsABlankValueAsAbsent(String blank) {
    Map<String, String> environment = complete();
    environment.put(WebConstants.ENV_API_TOKEN, blank);

    assertThatThrownBy(() -> Settings.from(environment::get))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(WebConstants.MSG_MISSING_SETTING.formatted(WebConstants.ENV_API_TOKEN));
  }

  @Test
  @DisplayName("the migrator reads the database settings without demanding an API token")
  void theMigratorNeedsNoApiToken() {
    Map<String, String> environment = complete();
    environment.remove(WebConstants.ENV_API_TOKEN);

    DatabaseSettings database = DatabaseSettings.from(environment::get);

    assertThat(database.url()).isEqualTo(URL);
    assertThat(database.user()).isEqualTo(USER);
    assertThat(database.password()).isEqualTo(PASSWORD);
  }

  @ParameterizedTest
  @DisplayName("the migrator still names a database setting it needs")
  @ValueSource(strings = {"DATABASE_URL", "DATABASE_USER", "DATABASE_PASSWORD"})
  void theMigratorNamesAMissingDatabaseSetting(String missing) {
    Map<String, String> environment = complete();
    environment.remove(missing);

    assertThatThrownBy(() -> DatabaseSettings.from(environment::get))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(WebConstants.MSG_MISSING_SETTING.formatted(missing));
  }

  @Test
  @DisplayName("refuses a token a reader could mistake for a JSON Web Token")
  void refusesATokenThatLooksLikeAJwt() {
    Map<String, String> environment = complete();
    environment.put(WebConstants.ENV_API_TOKEN, TOKEN_WITH_PERIOD);

    assertThatThrownBy(() -> Settings.from(environment::get))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(WebConstants.MSG_TOKEN_HAS_SEPARATOR);
  }
}
