package org.printscript.testkit;

import java.util.Optional;
import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.analyzer.NamingStyle;
import org.printscript.formatter.FormatterConfig;

/**
 * Test-only formatter and analyzer configurations. Production builds configs from the user's JSON
 * file ({@code toolchain.JsonPrintScriptConfigReader}); tests need ready-made ones instead.
 */
public final class TestConfigs {
  private TestConfigs() {}

  /**
   * Every spacing rule set to a typical value; the v1.1 brace and line-break rules left as written.
   */
  public static FormatterConfig formatterDefaults() {
    return new FormatterConfig(
        Optional.of(0),
        Optional.of(0),
        Optional.of(1),
        Optional.of(1),
        Optional.of(0),
        Optional.of(2),
        Optional.of(0),
        Optional.of(1),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  /**
   * No space around {@code ;}, one around {@code =} and operators, one blank line after each {@code
   * println}, two-space block indent; every other rule left as written.
   */
  public static FormatterConfig formatterOneBlankLineAfterPrintln() {
    return new FormatterConfig(
        Optional.of(0),
        Optional.of(0),
        Optional.of(1),
        Optional.of(1),
        Optional.of(1),
        Optional.of(2),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  /** Every analyzer rule on, with snake_case naming. */
  public static AnalyzerConfig analyzerDefaults() {
    return new AnalyzerConfig(NamingStyle.SNAKE_CASE, true, true, true);
  }
}
