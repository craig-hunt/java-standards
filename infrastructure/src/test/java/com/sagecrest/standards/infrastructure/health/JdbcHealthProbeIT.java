package com.sagecrest.standards.infrastructure.health;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.sagecrest.standards.infrastructure.persistence.PostgresFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JdbcHealthProbeIT extends PostgresFixture {

  @Test
  @DisplayName("completes a round trip rather than merely borrowing a connection")
  void completesARoundTrip() {
    assertThatCode(new JdbcHealthProbe(database())::ping).doesNotThrowAnyException();
  }
}
