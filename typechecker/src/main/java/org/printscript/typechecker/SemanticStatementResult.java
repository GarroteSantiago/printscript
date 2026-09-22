package org.printscript.typechecker;

import java.util.List;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.semantics.SemanticModel;

public record SemanticStatementResult(
    SemanticContext nextContext, SemanticModel semanticModel, List<Diagnostic> diagnostics) {
  public SemanticStatementResult {
    diagnostics = List.copyOf(diagnostics);
  }

  public boolean isSuccess() {
    return diagnostics.isEmpty();
  }
}
