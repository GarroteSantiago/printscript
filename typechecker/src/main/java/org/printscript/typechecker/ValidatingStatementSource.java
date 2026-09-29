package org.printscript.typechecker;

import org.printscript.ast.StatementSource;
import org.printscript.typetable.SemanticException;
import org.printscript.typetable.ValidatedStatement;
import org.printscript.typetable.ValidatedStatementSource;

/**
 * The sole production implementation of {@link ValidatedStatementSource}: wraps a raw {@link
 * StatementSource} and a {@link SemanticContext}, validating each statement as it's pulled and
 * threading the resulting context to the next pull internally, so no caller has to manage {@code
 * SemanticContext} state across a loop. Throws {@link SemanticException} the moment a statement
 * fails validation; there is no recovery, matching {@code StatementSyntaxReader}'s "first error
 * stops the stream" behavior one phase earlier.
 */
public final class ValidatingStatementSource implements ValidatedStatementSource {
  private final StatementSource statements;
  private SemanticContext context;

  public ValidatingStatementSource(StatementSource statements, SemanticContext initial) {
    this.statements = statements;
    this.context = initial;
  }

  @Override
  public boolean hasNext() {
    return statements.hasNext();
  }

  @Override
  public ValidatedStatement next() {
    var statement = statements.next();
    SemanticStatementResult result = context.validate(statement);
    if (!result.isSuccess()) {
      throw new SemanticException(result.diagnostics());
    }
    context = result.nextContext();
    return new ValidatedStatement(statement, result.semanticModel());
  }
}
