package org.printscript.typetable;

import java.util.List;
import org.printscript.diagnostics.Diagnostic;

/**
 * Thrown by a {@link ValidatedStatementSource} when a statement fails semantic validation. Carries
 * the {@link Diagnostic}s that describe the problem — this exception is only the transport
 * mechanism to a catch site (the {@code application} composition root), not the error
 * representation itself, mirroring {@code tokens.SyntaxException} one phase earlier. There is no
 * validation recovery: the first {@code SemanticException} stops the command.
 */
public final class SemanticException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final transient List<Diagnostic> diagnostics;

  public SemanticException(List<Diagnostic> diagnostics) {
    super(diagnostics.isEmpty() ? "semantic validation failed" : diagnostics.get(0).message());
    this.diagnostics = List.copyOf(diagnostics);
  }

  public List<Diagnostic> diagnostics() {
    return diagnostics;
  }
}
