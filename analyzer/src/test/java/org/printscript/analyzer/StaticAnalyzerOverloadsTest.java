package org.printscript.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.statements.IfStatementSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestConfigs;
import org.printscript.testkit.TestSources;
import org.printscript.typechecker.SemanticContext;
import org.printscript.typechecker.ValidatingStatementSource;

class StaticAnalyzerOverloadsTest {
  @Test
  public void analyzeAllAnalyzesEveryValidatedStatement() {
    var statements =
        new ValidatingStatementSource(
            TestSources.statementsOf("let badName: string = \"x\";\nprintln(\"hi \" + badName);"),
            SemanticContext.forVersion(false));
    List<Diagnostic> diagnostics = new ArrayList<>();

    new StaticAnalyzer(NamingStyleRules.v1())
        .analyzeAll(statements, TestConfigs.analyzerDefaults(), diagnostics::add);

    assertEquals(
        2,
        diagnostics.size(),
        "expected the naming diagnostic and the println-argument diagnostic");
  }

  @Test
  public void blockStatementOverloadAnalyzesNestedStatementsDirectly() {
    var statements =
        TestSources.statementsOf(
            """
            let flag: boolean = true;
            if (flag) {
              let badName: string = "x";
            }
            """,
            KeywordTable.v1_1());
    var semanticContext = SemanticContext.forVersion(true);
    var flagDeclarationResult = semanticContext.validate(statements.next());
    var ifStatement = (IfStatementSyntax) statements.next();
    var ifResult = flagDeclarationResult.nextContext().validate(ifStatement);

    List<Diagnostic> diagnostics = new ArrayList<>();
    new StaticAnalyzer(NamingStyleRules.v1())
        .analyze(
            ifStatement.thenBlock(),
            ifResult.semanticModel(),
            TestConfigs.analyzerDefaults(),
            diagnostics::add);

    assertEquals(
        1,
        diagnostics.size(),
        "expected the block overload to analyze its nested statement directly");
  }
}
