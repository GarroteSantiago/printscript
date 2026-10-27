package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.diagnostics.DiagnosticException;

class InterpreterV11Test {
  private static final InputPort FAILING_INPUT =
      prompt -> {
        throw new AssertionError("unexpected read from stdin");
      };
  private static final EnvironmentPort EMPTY_ENVIRONMENT = name -> Optional.empty();

  private List<String> run(String source, InputPort input, EnvironmentPort env) {
    List<String> output = new ArrayList<>();
    var interpreter = new Interpreter(output::add, ArithmeticOperators.v1(), input, env);
    Execution.run(interpreter, Execution.validatedV11(source));
    return output;
  }

  private DiagnosticException runAndCaptureFailure(
      String source, InputPort input, EnvironmentPort env) {
    return assertThrows(
        DiagnosticException.class, () -> run(source, input, env), "expected execution to fail");
  }

  @Test
  public void executesThenBranchWhenConditionIsTrue() {
    List<String> output =
        run(
            """
            let flag: boolean = true;
            if (flag) {
              println("yes");
            } else {
              println("no");
            }
            """,
            FAILING_INPUT,
            EMPTY_ENVIRONMENT);

    assertEquals(List.of("yes"), output, "expected the then-branch to run");
  }

  @Test
  public void executesElseBranchWhenConditionIsFalse() {
    List<String> output =
        run(
            """
            let flag: boolean = false;
            if (flag) {
              println("yes");
            } else {
              println("no");
            }
            """,
            FAILING_INPUT,
            EMPTY_ENVIRONMENT);

    assertEquals(List.of("no"), output, "expected the else-branch to run");
  }

  @Test
  public void variablesDeclaredInsideBlockDoNotLeakButAssignmentsPersist() {
    List<String> output =
        run(
            """
            let flag: boolean = true;
            let counter: number = 1;
            if (flag) {
              counter = 2;
            }
            println(counter);
            """,
            FAILING_INPUT,
            EMPTY_ENVIRONMENT);

    assertEquals(List.of("2"), output, "expected the in-block assignment to persist");
  }

  @Test
  public void readInputParsesBooleanFromStdin() {
    List<String> output =
        run(
            """
            let flag: boolean = readInput("prompt");
            println(flag);
            """,
            prompt -> "true",
            EMPTY_ENVIRONMENT);

    assertEquals(List.of("true"), output, "expected the stdin value parsed as boolean");
  }

  @Test
  public void readInputFailsOnUnparseableBoolean() {
    DiagnosticException failure =
        runAndCaptureFailure(
            "let flag: boolean = readInput(\"prompt\");",
            prompt -> "not-a-boolean",
            EMPTY_ENVIRONMENT);

    assertEquals(
        "Expected a boolean but got 'not-a-boolean'",
        failure.getMessage(),
        "expected an unparseable-boolean failure message");
  }

  @Test
  public void readEnvParsesNumberFromEnvironment() {
    List<String> output =
        run(
            """
            let count: number = readEnv("COUNT");
            println(count);
            """,
            FAILING_INPUT,
            name -> "COUNT".equals(name) ? Optional.of("42") : Optional.empty());

    assertEquals(List.of("42"), output, "expected the env value parsed as number");
  }

  @Test
  public void readEnvFailsWhenVariableIsMissing() {
    DiagnosticException failure =
        runAndCaptureFailure(
            "let count: number = readEnv(\"MISSING\");", FAILING_INPUT, EMPTY_ENVIRONMENT);

    assertEquals(
        "Environment variable 'MISSING' is not set",
        failure.getMessage(),
        "expected a missing-env-variable failure message");
  }
}
