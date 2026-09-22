package org.printscript.diagnostics;

/**
 * Which pipeline stage produced a {@link Diagnostic}: scanning/parsing, semantic validation,
 * interpretation, formatting, static analysis, or the {@code application} composition root itself
 * (e.g. an unsupported {@code LanguageVersion}).
 */
public enum Phase {
  SYNTAX,
  SEMANTIC,
  RUNTIME,
  FORMATTER,
  ANALYZER,
  APPLICATION
}
