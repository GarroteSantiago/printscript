package org.printscript.tokens;

import org.printscript.diagnostics.Diagnostic;

/**
 * Thrown by the lexer or parser on the first invalid character, unterminated literal, or malformed
 * statement. Carries the {@link Diagnostic} that actually describes the problem — this exception is
 * only the transport mechanism to a catch site (the {@code toolchain} composition root), not the
 * error representation itself. There is no parser recovery: the first {@code SyntaxException} stops
 * the command.
 */
public final class SyntaxException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final transient Diagnostic diagnostic;

  public SyntaxException(Diagnostic diagnostic) {
    super(diagnostic.message());
    this.diagnostic = diagnostic;
  }

  public Diagnostic diagnostic() {
    return diagnostic;
  }
}
