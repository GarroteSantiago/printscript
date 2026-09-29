package org.printscript.repl;

import org.printscript.typetable.ValidatedStatement;
import org.printscript.typetable.ValidatedStatementSource;

/**
 * Adapts one already-validated statement into the {@link ValidatedStatementSource} port {@link
 * org.printscript.interpreter.Interpreter#executeAll} expects, so a REPL can execute a single
 * statement through the exact same public entry point {@code toolchain.PrintScript} uses for a
 * whole program, without needing a new interpreter API. Yields its one statement, then signals
 * exhaustion.
 */
public final class SingleValidatedStatementSource implements ValidatedStatementSource {
  private final ValidatedStatement statement;
  private boolean consumed;

  public SingleValidatedStatementSource(ValidatedStatement statement) {
    this.statement = statement;
  }

  @Override
  public boolean hasNext() {
    return !consumed;
  }

  @Override
  public ValidatedStatement next() {
    consumed = true;
    return statement;
  }
}
