package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.diagnostics.DiagnosticException;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;
import org.printscript.typechecker.SemanticContext;
import org.printscript.typechecker.SemanticModelBuilder;
import org.printscript.typechecker.ValidatingStatementSource;
import org.printscript.typetable.BuiltinRegistry;
import org.printscript.typetable.SemanticModel;

/**
 * The whole-program and streaming entry points ({@link Interpreter#execute} and {@link
 * Interpreter#executeAll}), plus evaluation paths the statement-level tests don't reach.
 */
class InterpreterEntryPointsTest {
  private final List<String> output = new ArrayList<>();

  private Interpreter interpreter(InputPort input) {
    return new Interpreter(output::add, ArithmeticOperators.v1(), input, name -> Optional.empty());
  }

  private List<String> executeAll(String source) {
    var statements =
        new ValidatingStatementSource(
            TestSources.statementsOf(source, KeywordTable.v1_1()),
            SemanticContext.forVersion(true));
    interpreter(prompt -> "").executeAll(statements, RuntimeEnvironment.empty());
    return output;
  }

  @Test
  public void executeRunsEveryStatementOfAProgram() {
    // Call-free on purpose: SemanticModelBuilder.build() keeps only diagnostics, not resolved
    // calls.
    var program = TestSources.programOf("let x: number = 1;\nlet y: number = x + 2;");
    SemanticModel model = new SemanticModelBuilder(BuiltinRegistry.v1()).build(program);

    RuntimeEnvironment environment = interpreter(prompt -> "").execute(program, model);

    assertEquals(
        Optional.of(new RuntimeValue.NumberValue(new BigDecimal("3"))),
        environment.find("y"),
        "expected the second statement to see the first one's variable");
  }

  @Test
  public void executeAllRunsEveryValidatedStatement() {
    assertEquals(
        List.of("3"),
        executeAll("let x: number = 1 + 2;\nprintln(x);"),
        "expected the streamed statements to run in order");
  }

  @Test
  public void anIfWithoutElseDoesNothingWhenFalse() {
    assertEquals(
        List.of("after"),
        executeAll("if (false) {\n  println(\"then\");\n}\nprintln(\"after\");"),
        "expected nothing to run for a false condition without an else");
  }

  @Test
  public void addingAStringAndANumberConcatenates() {
    assertEquals(
        List.of("a1"),
        executeAll("println(\"a\" + 1);"),
        "expected string concatenation with the number's text");
  }

  @Test
  public void readInputParsesFalseAsABoolean() {
    var statements =
        new ValidatingStatementSource(
            TestSources.statementsOf(
                "let flag: boolean = readInput(\"?\");\nprintln(flag);", KeywordTable.v1_1()),
            SemanticContext.forVersion(true));

    interpreter(prompt -> "false").executeAll(statements, RuntimeEnvironment.empty());

    assertEquals(List.of("false"), output, "expected 'false' read as the boolean false");
  }

  private DiagnosticException unresolvedCallFailure() {
    var program = TestSources.programOf("println(1);");
    SemanticModel unvalidated = SemanticModel.builder().build();
    return assertThrows(
        DiagnosticException.class,
        () -> interpreter(prompt -> "").execute(program, unvalidated),
        "expected a call the semantic model never resolved to fail");
  }

  @Test
  public void aCallTheSemanticModelNeverResolvedFails() {
    assertEquals(
        "Unknown callable 'println'",
        unresolvedCallFailure().getMessage(),
        "expected an unknown-callable runtime error");
  }
}
