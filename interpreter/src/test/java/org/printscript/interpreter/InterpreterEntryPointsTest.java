package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.diagnostics.DiagnosticException;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;
import org.printscript.typechecker.SemanticContext;
import org.printscript.typechecker.ValidatingStatementSource;
import org.printscript.typetable.SemanticModel;
import org.printscript.typetable.ValidatedStatement;
import org.printscript.typetable.ValidatedStatementSource;

/**
 * The streaming entry point ({@link Interpreter#executeAll}), plus evaluation paths the
 * statement-level tests don't reach.
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

  /** A source yielding one statement paired with an empty model, as if validation were skipped. */
  private static ValidatedStatementSource unvalidated(String source) {
    Deque<ValidatedStatement> remaining = new ArrayDeque<>();
    remaining.add(
        new ValidatedStatement(
            TestSources.statementsOf(source).next(), SemanticModel.builder().build()));
    return new ValidatedStatementSource() {
      @Override
      public boolean hasNext() {
        return !remaining.isEmpty();
      }

      @Override
      public ValidatedStatement next() {
        return remaining.pop();
      }
    };
  }

  private DiagnosticException unresolvedCallFailure() {
    return assertThrows(
        DiagnosticException.class,
        () ->
            interpreter(prompt -> "")
                .executeAll(unvalidated("println(1);"), RuntimeEnvironment.empty()),
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
