package org.printscript.diagnostics;

import java.util.List;

/**
 * The one exception every pipeline stage throws to stop on a problem in user code: the lexer and
 * parser on a syntax error, the typechecker on a semantic error, the interpreter on a runtime
 * failure. It carries the {@link Diagnostic}s that describe the problem and is only the transport
 * mechanism to a catch site, never a substitute for them — which stage failed is each diagnostic's
 * {@link Diagnostic#phase()}, so callers catch this single type instead of knowing every stage's
 * own error type. There is no recovery: the first one stops the command.
 */
public final class DiagnosticException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final transient List<Diagnostic> diagnostics;

  public DiagnosticException(Diagnostic diagnostic) {
    this(List.of(diagnostic));
  }

  /**
   * Wraps one or more diagnostics; the exception's message is the first one's.
   *
   * @throws IllegalArgumentException if {@code diagnostics} is empty — a failure must say why
   */
  public DiagnosticException(List<Diagnostic> diagnostics) {
    super(firstMessage(diagnostics));
    this.diagnostics = List.copyOf(diagnostics);
  }

  public List<Diagnostic> diagnostics() {
    return diagnostics;
  }

  private static String firstMessage(List<Diagnostic> diagnostics) {
    if (diagnostics.isEmpty()) {
      throw new IllegalArgumentException("A DiagnosticException needs at least one diagnostic");
    }
    return diagnostics.getFirst().message();
  }
}
