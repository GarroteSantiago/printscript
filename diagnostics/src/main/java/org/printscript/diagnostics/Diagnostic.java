package org.printscript.diagnostics;

import org.printscript.source.SourceSpan;

/**
 * A structured description of a problem in user code, reported by some pipeline {@link Phase} at a
 * {@link SourceSpan}. This is the vocabulary every stage (lexer, parser, semantics, interpreter,
 * analyzer) uses to surface user-facing problems — {@link DiagnosticException} exists only to carry
 * diagnostics up to a catch site, never as a substitute for one. {@link #code} is a
 * machine-readable reason for the few callers that react to a specific problem; most diagnostics
 * carry {@link DiagnosticCode#UNSPECIFIED}.
 */
public record Diagnostic(
    Severity severity, Phase phase, String message, SourceSpan span, DiagnosticCode code) {

  /** A diagnostic with no specific {@link DiagnosticCode}. */
  public Diagnostic(Severity severity, Phase phase, String message, SourceSpan span) {
    this(severity, phase, message, span, DiagnosticCode.UNSPECIFIED);
  }

  /** Convenience factory for the common case: an {@link Severity#ERROR} diagnostic. */
  public static Diagnostic error(Phase phase, String message, SourceSpan span) {
    return new Diagnostic(Severity.ERROR, phase, message, span);
  }

  /** This same diagnostic, carrying {@code newCode} instead. */
  public Diagnostic withCode(DiagnosticCode newCode) {
    return new Diagnostic(severity, phase, message, span, newCode);
  }
}
