package org.sonarcrypto.utils.sonar;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.Nullable;

public class TextUtils {

  /** Joins items of an iterable to a comma separated string, e.g. {@code "Foo, Bar, and Baz"}. */
  public static String join(Iterable<?> values, @Nullable String lastDelimiter) {
    return join(values, lastDelimiter, null);
  }

  /** Joins items of an iterable to a comma separated string, e.g. {@code "Foo, Bar, and Baz"}. */
  public static String join(
      Iterable<?> values, @Nullable String lastDelimiter, @Nullable String suffix) {
    return join(StreamSupport.stream(values.spliterator(), false), lastDelimiter, suffix);
  }

  /** Joins items of an iterable to a comma separated string, e.g. {@code "Foo, Bar, and Baz"}. */
  public static String join(Stream<?> values, @Nullable String lastSeparator) {
    return join(values, lastSeparator, null);
  }

  /** Joins items of an iterable to a comma separated string, e.g. {@code "Foo, Bar, and Baz"}. */
  public static String join(
      Stream<?> values, @Nullable String lastSeparator, @Nullable String suffix) {
    final var iterator = values.iterator();

    if (!iterator.hasNext()) {
      return "";
    }

    final var sb = new StringBuilder();

    var valueCount = 0;

    while (iterator.hasNext()) {
      if (++valueCount > 1) {
        sb.append(", ");
      }

      final var next = iterator.next();

      if (valueCount > 2
          && lastSeparator != null
          && !lastSeparator.isEmpty()
          && !iterator.hasNext()) {
        sb.append(lastSeparator).append(' ');
      }

      if (next instanceof String s) {
        sb.append(quote(s));
      } else {
        sb.append(next);
      }
    }

    if (valueCount > 1 && suffix != null && !suffix.isEmpty()) {
      sb.append(", ").append(suffix);
    }

    return sb.toString();
  }

  /**
   * Quotes the given string and escapes special characters.
   *
   * <p>The value {@code "Foo \"Bar\"\nBaz"}, for example, becomes {@code "\"Foo
   * \\\"Bar\\\"\\nBaz\""}.
   *
   * @see StringEscapeUtils#escapeJava
   */
  public static String quote(String value) {
    return "\"" + StringEscapeUtils.escapeJava(value) + "\"";
  }
}
