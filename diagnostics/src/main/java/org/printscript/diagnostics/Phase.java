package org.printscript.diagnostics;

/**
 * Which pipeline stage produced a {@link Diagnostic}: scanning/parsing, semantic validation,
 * interpretation, formatting, or static analysis.
 */
public enum Phase {
  SYNTAX,
  SEMANTIC,
  RUNTIME,
  FORMATTER,
  ANALYZER
}
