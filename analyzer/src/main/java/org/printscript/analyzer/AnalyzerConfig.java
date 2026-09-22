package org.printscript.analyzer;

/**
 * Which {@link StaticAnalyzer} rules are enabled and how they're configured. Unlike {@link
 * org.printscript.formatter.FormatterConfigProvider}'s absent-means-untouched rules, every field
 * here is a concrete value (not {@code Optional}) — an analyzer rule is either on with a specific
 * setting or off via its {@code checkX}/{@code restrictX} flag, there is no third "unconfigured"
 * state to preserve.
 */
public record AnalyzerConfig(
    NamingStyle namingStyle,
    boolean checkIdentifierNaming,
    boolean restrictPrintlnToSimpleArguments,
    boolean restrictReadInputToSimpleArguments) {
  public static AnalyzerConfig defaults() {
    return new AnalyzerConfig(NamingStyle.SNAKE_CASE, true, true, true);
  }
}
