package org.printscript.diagnostics;

import org.printscript.source.SourceSpan;

/**
 * A structured description of a problem in user code, reported by some pipeline {@link Phase} at a
 * {@link SourceSpan}. This is the vocabulary every stage (lexer, parser, semantics, interpreter,
 * analyzer) uses to surface user-facing problems — exceptions such as {@code SyntaxException} and
 * {@code RuntimeFailure} exist only to carry a {@code Diagnostic} up to a catch site, never as a
 * substitute for one.
 */
public record Diagnostic(Severity severity, Phase phase, String message, SourceSpan span) {
  /** Convenience factory for the common case: an {@link Severity#ERROR} diagnostic. */
  public static Diagnostic error(Phase phase, String message, SourceSpan span) {
    return new Diagnostic(Severity.ERROR, phase, message, span);
  }
}
