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
import org.printscript.typechecker.SemanticModelBuilder;
import org.printscript.types.TypeAnnotationTable;
import org.printscript.typetable.BuiltinRegistry;

class StaticAnalyzerOverloadsTest {
  @Test
  void programOverloadAnalyzesEveryStatementInTheProgram() {
    var program = TestSources.programOf("let badName: string = \"x\";\nprintln(badName);");
    var model = new SemanticModelBuilder(BuiltinRegistry.v1()).build(program);

    var diagnostics = new StaticAnalyzer().analyze(program, model, AnalyzerConfig.defaults());

    assertEquals(
        1, diagnostics.size(), "expected the naming-style diagnostic from the program overload");
  }

  @Test
  void statementOverloadReturnsCollectedDiagnostics() {
    var statement = TestSources.statementsOf("let badName: string = \"x\";").next();
    var semantic = SemanticContext.empty(BuiltinRegistry.v1()).validate(statement);

    var diagnostics =
        new StaticAnalyzer()
            .analyze(statement, semantic.semanticModel(), AnalyzerConfig.defaults());

    assertEquals(
        1, diagnostics.size(), "expected the naming-style diagnostic from the statement overload");
  }

  @Test
  void blockStatementOverloadAnalyzesNestedStatementsDirectly() {
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
