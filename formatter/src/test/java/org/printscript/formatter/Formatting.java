package org.printscript.formatter;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.printscript.tokens.TokenSource;

/** Test helper: runs {@link PrintScriptFormatter#formatAll} and returns what it wrote. */
final class Formatting {
  private Formatting() {}

  static String format(TokenSource tokens, FormatterConfigProvider config) {
    return format(new PrintScriptFormatter(SpacingRules.v1()), tokens, config);
  }

  static String formatV11(TokenSource tokens, FormatterConfigProvider config) {
    return format(new PrintScriptFormatter(SpacingRules.v1_1()), tokens, config);
  }

  private static String format(
      PrintScriptFormatter formatter, TokenSource tokens, FormatterConfigProvider config) {
    StringBuilder out = new StringBuilder();
    try {
      formatter.formatAll(tokens, config, out);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    return out.toString();
  }
}
