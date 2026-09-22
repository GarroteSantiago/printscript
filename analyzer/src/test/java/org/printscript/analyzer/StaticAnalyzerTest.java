package org.printscript.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.semantics.BuiltinRegistry;
import org.printscript.testkit.TestSources;
import org.printscript.typechecker.SemanticContext;

class StaticAnalyzerTest {
  private List<Diagnostic> diagnostics;

  @BeforeEach
  void reportConfiguredStyleAndPrintlnPolicyViolations() {
    var statements =
        TestSources.statementsOf(
            """
                let badName: string = "hello";
                println("hello " + badName);
                """);
    var semanticContext = SemanticContext.empty(BuiltinRegistry.v1());
    diagnostics = new ArrayList<>();

    while (statements.hasNext()) {
      var statement = statements.next();
      var semantic = semanticContext.validate(statement);
      new StaticAnalyzer()
          .analyze(
              statement, semantic.semanticModel(), AnalyzerConfig.defaults(), diagnostics::add);
      semanticContext = semantic.nextContext();
    }
  }

  @Test
  void reportsExpectedDiagnosticCount() {
    assertEquals(2, diagnostics.size(), "expected two diagnostics");
  }

  @Test
  void reportsNamingStyleViolationFirst() {
    assertEquals(
        "Identifier 'badName' does not match SNAKE_CASE",
        diagnostics.get(0).message(),
        "expected naming style diagnostic first");
  }

  @Test
  void reportsPrintlnArgumentViolationSecond() {
    assertEquals(
        "println argument must be an identifier or literal",
        diagnostics.get(1).message(),
        "expected println policy diagnostic second");
  }
}
