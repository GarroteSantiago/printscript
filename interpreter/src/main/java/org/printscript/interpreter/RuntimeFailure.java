package org.printscript.interpreter;

import org.printscript.diagnostics.Diagnostic;

/**
 * Thrown when a validated statement fails at runtime (division by zero, an unset environment
 * variable, malformed {@code readInput}/{@code readEnv} input, ...). As with {@code
 * tokens.SyntaxException}, this carries the {@link Diagnostic} that describes the problem and is
 * only the transport to the catch site in {@code application.PrintScript}; execution stops
 * immediately on the first one.
 */
public final class RuntimeFailure extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final transient Diagnostic diagnostic;

  public RuntimeFailure(Diagnostic diagnostic) {
    super(diagnostic.message());
    this.diagnostic = diagnostic;
  }

  public Diagnostic diagnostic() {
    return diagnostic;
  }
}
