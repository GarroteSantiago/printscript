package org.printscript.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.statements.IfStatementSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;
import org.printscript.typechecker.SemanticContext;
import org.printscript.typechecker.ValidatingStatementSource;
import org.printscript.types.TypeAnnotationTable;
import org.printscript.typetable.BuiltinRegistry;

class StaticAnalyzerOverloadsTest {
  @Test
  public void analyzeAllAnalyzesEveryValidatedStatement() {
    var statements =
        new ValidatingStatementSource(
            TestSources.statementsOf("let badName: string = \"x\";\nprintln(\"hi \" + badName);"),
            SemanticContext.forVersion(false));
    List<Diagnostic> diagnostics = new ArrayList<>();

    new StaticAnalyzer().analyzeAll(statements, AnalyzerConfig.defaults(), diagnostics::add);

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
    var semanticContext = SemanticContext.empty(BuiltinRegistry.v1_1(), TypeAnnotationTable.v1_1());
    var flagDeclarationResult = semanticContext.validate(statements.next());
    var ifStatement = (IfStatementSyntax) statements.next();
    var ifResult = flagDeclarationResult.nextContext().validate(ifStatement);

    List<Diagnostic> diagnostics = new ArrayList<>();
    new StaticAnalyzer()
        .analyze(
            ifStatement.thenBlock(),
            ifResult.semanticModel(),
            AnalyzerConfig.defaults(),
            diagnostics::add);

    assertEquals(
        1,
        diagnostics.size(),
        "expected the block overload to analyze its nested statement directly");
  }
}
