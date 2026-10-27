package org.printscript.typechecker;

import java.util.List;
import org.printscript.ast.StatementSource;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;

/**
 * Test helper: validates a whole source one statement at a time, the way production does, and
 * returns the diagnostics of the first statement that fails (none if every statement is valid).
 */
final class Validation {
  private Validation() {}

  static List<Diagnostic> diagnosticsOf(String source) {
    return diagnosticsOf(TestSources.statementsOf(source), false);
  }

  static List<Diagnostic> diagnosticsOfV11(String source) {
    return diagnosticsOf(TestSources.statementsOf(source, KeywordTable.v1_1()), true);
  }

  private static List<Diagnostic> diagnosticsOf(StatementSource statements, boolean v11) {
    SemanticContext context = SemanticContext.forVersion(v11);
    while (statements.hasNext()) {
      SemanticStatementResult result = context.validate(statements.next());
      if (!result.isSuccess()) {
        return result.diagnostics();
      }
      context = result.nextContext();
    }
    return List.of();
  }
}
