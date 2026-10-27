package org.printscript.diagnostics;

/**
 * A machine-readable reason for a {@link Diagnostic}, for callers that must react to a specific
 * kind of problem without matching on its human-readable message. Most diagnostics need none and
 * carry {@link #UNSPECIFIED}.
 */
public enum DiagnosticCode {
  /** No specific code: the message and phase are all a caller needs. */
  UNSPECIFIED,
  /**
   * The input ended while a construct was still unfinished (a missing {@code ;}, an unclosed block,
   * a trailing operator). More input could make it valid, which is how the REPL knows to read
   * another line instead of reporting an error.
   */
  UNEXPECTED_END_OF_INPUT
}
