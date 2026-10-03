package com.sagecrest.standards.domain.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextLengthTest {

  /** One code point, two UTF-16 code units. The distinction is the whole point. */
  private static final String EMOJI = "😀";

  private static final String LATIN = "ab";
  private static final int ONE = 1;
  private static final int TWO = 2;
  private static final int NONE = 0;

  @Test
  @DisplayName("counts a supplementary character once, not twice")
  void countsSupplementaryCharacterOnce() {
    assertThat(EMOJI.length()).isEqualTo(TWO);
    assertThat(TextLength.countCodePoints(EMOJI)).isEqualTo(ONE);
  }

  @Test
  void countsEachLatinCharacterOnce() {
    assertThat(TextLength.countCodePoints(LATIN)).isEqualTo(TWO);
  }

  @Test
  void countsNothingInAnEmptyString() {
    assertThat(TextLength.countCodePoints("")).isEqualTo(NONE);
  }
}
