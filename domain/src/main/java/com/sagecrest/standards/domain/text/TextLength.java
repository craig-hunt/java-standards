package com.sagecrest.standards.domain.text;

/**
 * Counts the characters a length limit governs.
 *
 * <p>A limit expressed in {@code String.length()} counts UTF-16 code units, so it would reject a
 * title an emoji makes two units longer while accepting a longer one made of Latin letters.
 * Counting code points puts the limit where a reader would put it, and in the same place as the Go
 * and C# siblings, which count runes. The boundary stays identical across all three
 * implementations.
 */
public final class TextLength {

  private static final int START = 0;

  public static int countCodePoints(String value) {
    return value.codePointCount(START, value.length());
  }

  private TextLength() {}
}
