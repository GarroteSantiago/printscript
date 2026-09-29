package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.testkit.TestSources;
import org.printscript.typechecker.SemanticContext;
import org.printscript.typechecker.SemanticStatementResult;
import org.printscript.typetable.BuiltinRegistry;

class InterpreterDeclarationTest {
  private List<String> run(String source) {
    List<String> output = new ArrayList<>();
    var statements = TestSources.statementsOf(source);
    var semanticContext = SemanticContext.empty(BuiltinRegistry.v1());
    var interpreter = new Interpreter(output::add);
    var environment = RuntimeEnvironment.empty();
    while (statements.hasNext()) {
      StatementSyntax statement = statements.next();
      SemanticStatementResult semantic = semanticContext.validate(statement);
      environment = interpreter.executeStatement(statement, environment, semantic.semanticModel());
      semanticContext = semantic.nextContext();
    }
    return output;
  }

  @Test
  public void declarationWithoutInitializerCanBeAssignedAndPrinted() {
    List<String> output =
        run(
            """
            let result: number;
            result = 5;
            println(result);
            """);

    assertEquals(List.of("5"), output, "expected the later-assigned value to print");
  }

  @Test
  public void printlnAcceptsANumberDirectly() {
    List<String> output =
        run(
            """
            let result: number = 5 * 5 - 8;
            println(result);
            """);

    assertEquals(List.of("17"), output, "expected the number stringified directly");
  }
}
