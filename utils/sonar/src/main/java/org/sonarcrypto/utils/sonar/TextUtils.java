package org.sonarcrypto.utils.sonar;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class TextUtils {
  private TextUtils() {
    // Utility class
  }

  /**
   * Quotes the given string and escapes special characters.
   *
   * <p>The value {@code "Foo \"Bar\"\nBaz"}, for example, becomes {@code "\"Foo
   * \\\"Bar\\\"\\nBaz\""}.
   *
   * @see #escape
   */
  public static String quote(String value) {
    return "\"" + escape(value) + "\"";
  }

  /**
   * Escapes the given string and escapes special characters.
   *
   * <p>The value {@code "Foo \"Bar\"\nBaz"}, for example, becomes {@code "Foo \\\"Bar\\\"\\nBaz"}.
   *
   * @see StringEscapeUtils#escapeJava
   */
  public static String escape(String value) {
    return StringEscapeUtils.escapeJava(value);
  }
}
