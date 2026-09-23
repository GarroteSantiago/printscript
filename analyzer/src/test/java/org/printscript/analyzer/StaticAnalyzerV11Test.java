package org.printscript.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;
import org.printscript.typechecker.SemanticContext;
import org.printscript.types.TypeAnnotationTable;
import org.printscript.typetable.BuiltinRegistry;

class StaticAnalyzerV11Test {
  private List<Diagnostic> analyze(String source, AnalyzerConfig config) {
    var statements = TestSources.statementsOf(source, KeywordTable.v1_1());
    var semanticContext = SemanticContext.empty(BuiltinRegistry.v1_1(), TypeAnnotationTable.v1_1());
    List<Diagnostic> diagnostics = new ArrayList<>();
    while (statements.hasNext()) {
      var statement = statements.next();
      var semantic = semanticContext.validate(statement);
      new StaticAnalyzer().analyze(statement, semantic.semanticModel(), config, diagnostics::add);
      semanticContext = semantic.nextContext();
    }
    return diagnostics;
  }

  @Test
  void reportsReadInputCalledWithComposedExpression() {
    var diagnostics =
        analyze(
            "let prompt: string = \"p\";\nlet value: string = readInput(prompt + \"!\");",
            new AnalyzerConfig(NamingStyle.SNAKE_CASE, true, true, true));

    assertEquals(1, diagnostics.size(), "expected one diagnostic");
  }

  @Test
  void reportsReadInputComposedExpressionMessage() {
    var diagnostics =
        analyze(
            "let prompt: string = \"p\";\nlet value: string = readInput(prompt + \"!\");",
            new AnalyzerConfig(NamingStyle.SNAKE_CASE, true, true, true));

    assertEquals(
        "readInput argument must be an identifier or literal",
        diagnostics.getFirst().message(),
        "expected readInput argument-shape diagnostic message");
  }

  @Test
  void allowsReadInputCalledWithLiteralArgument() {
    var diagnostics =
        analyze(
            "let value: string = readInput(\"prompt\");",
            new AnalyzerConfig(NamingStyle.SNAKE_CASE, true, true, true));

    assertTrue(diagnostics.isEmpty(), "expected no diagnostics");
  }

  @Test
  void doesNotReportWhenRuleIsDisabled() {
    var diagnostics =
        analyze(
            "let prompt: string = \"p\";\nlet value: string = readInput(prompt + \"!\");",
            new AnalyzerConfig(NamingStyle.SNAKE_CASE, true, true, false));

    assertTrue(diagnostics.isEmpty(), "expected no diagnostics when the rule is disabled");
  }

  @Test
  void reportsReadInputArgumentShapeWhenNestedInsideAnIfBlock() {
    var diagnostics =
        analyze(
            """
            let prompt: string = "p";
            let flag: boolean = true;
            if (flag) {
              let value: string = readInput(prompt + "!");
            }
            """,
            new AnalyzerConfig(NamingStyle.SNAKE_CASE, true, true, true));

    assertEquals(1, diagnostics.size(), "expected the rule to fire inside a nested block");
  }

  @Test
  void reportsReadInputArgumentShapeWhenNestedInsideAnElseBlock() {
    var diagnostics =
        analyze(
            """
            let prompt: string = "p";
            let flag: boolean = false;
            if (flag) {
              println("yes");
            } else {
              let value: string = readInput(prompt + "!");
            }
            """,
            new AnalyzerConfig(NamingStyle.SNAKE_CASE, true, true, true));

    assertEquals(1, diagnostics.size(), "expected the rule to fire inside a nested else block");
  }

  @Test
  void reportsReadInputArgumentShapeWhenUsedAsAnAssignmentValue() {
    var diagnostics =
        analyze(
            """
            let prompt: string = "p";
            let value: string = "x";
            value = readInput(prompt + "!");
            """,
            new AnalyzerConfig(NamingStyle.SNAKE_CASE, true, true, true));

    assertEquals(1, diagnostics.size(), "expected the assignment's call argument to be checked");
  }
}
