package org.printscript.diagnostics;

import org.printscript.source.SourceSpan;

/**
 * A structured description of a problem in user code, reported by some pipeline {@link Phase} at a
 * {@link SourceSpan}. This is the vocabulary every stage (lexer, parser, semantics, interpreter,
 * analyzer) uses to surface user-facing problems — {@link DiagnosticException} exists only to carry
 * diagnostics up to a catch site, never as a substitute for one.
 */
public record Diagnostic(Severity severity, Phase phase, String message, SourceSpan span) {
  /** Convenience factory for the common case: an {@link Severity#ERROR} diagnostic. */
  public static Diagnostic error(Phase phase, String message, SourceSpan span) {
    return new Diagnostic(Severity.ERROR, phase, message, span);
  }
}
