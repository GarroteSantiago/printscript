package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.expressions.BinaryOperator;
import org.printscript.diagnostics.DiagnosticException;
import org.printscript.diagnostics.Phase;
import org.printscript.diagnostics.Severity;

class InterpreterRuntimeFailureTest {
  private static final InputPort FAILING_INPUT =
      prompt -> {
        throw new AssertionError("unexpected read from stdin");
      };
  private static final EnvironmentPort EMPTY_ENVIRONMENT = name -> Optional.empty();

  private DiagnosticException runAndCaptureFailure(
      String source, InputPort input, EnvironmentPort env) {
    return assertThrows(
        DiagnosticException.class,
        () -> {
          List<String> output = new ArrayList<>();
          var interpreter = new Interpreter(output::add, ArithmeticOperators.v1(), input, env);
          Execution.run(interpreter, Execution.validated(source));
        },
        "expected execution to fail");
  }

  @Test
  public void divisionByZeroThrowsARuntimeFailure() {
    DiagnosticException failure =
        runAndCaptureFailure("let result: number = 1 / 0;", FAILING_INPUT, EMPTY_ENVIRONMENT);

    assertEquals("Division by zero", failure.getMessage(), "expected a division-by-zero message");
  }

  @Test
  public void divisionByZeroDiagnosticHasErrorSeverity() {
    DiagnosticException failure =
        runAndCaptureFailure("let result: number = 1 / 0;", FAILING_INPUT, EMPTY_ENVIRONMENT);

    assertEquals(
        Severity.ERROR, failure.diagnostics().getFirst().severity(), "expected an error severity");
  }

  @Test
  public void divisionByZeroDiagnosticIsFromTheRuntimePhase() {
    DiagnosticException failure =
        runAndCaptureFailure("let result: number = 1 / 0;", FAILING_INPUT, EMPTY_ENVIRONMENT);

    assertEquals(
        Phase.RUNTIME, failure.diagnostics().getFirst().phase(), "expected the runtime phase");
  }

  @Test
  public void readInputFailsOnUnparseableNumber() {
    DiagnosticException failure =
        runV11AndCaptureFailure("let count: number = readInput(\"prompt\");");

    assertEquals(
        "Expected a number but got 'not-a-number'",
        failure.getMessage(),
        "expected an unparseable-number failure message");
  }

  private DiagnosticException runV11AndCaptureFailure(String source) {
    return assertThrows(
        DiagnosticException.class,
        () -> {
          List<String> output = new ArrayList<>();
          var interpreter =
              new Interpreter(
                  output::add,
                  ArithmeticOperators.v1(),
                  prompt -> "not-a-number",
                  EMPTY_ENVIRONMENT);
          Execution.run(interpreter, Execution.validatedV11(source));
        },
        "expected execution to fail");
  }

  @Test
  public void referencingAVariableDeclaredWithoutAnInitializerBeforeItIsAssignedFails() {
    DiagnosticException failure =
        runAndCaptureFailure(
            """
            let result: number;
            println(result);
            """,
            FAILING_INPUT,
            EMPTY_ENVIRONMENT);

    assertEquals(
        "Variable 'result' is not declared",
        failure.getMessage(),
        "expected an undeclared-variable failure message");
  }

  @Test
  public void readInputWithoutAnInjectedPortThrowsRuntimeFailure() {
    List<String> output = new ArrayList<>();
    var interpreter = new Interpreter(output::add, ArithmeticOperators.v1());
    var statements = Execution.validatedV11("let name: string = readInput(\"prompt\");");

    assertThrows(
        DiagnosticException.class,
        () -> Execution.run(interpreter, statements),
        "expected readInput to fail as a structured DiagnosticException without an injected input port");
  }

  @Test
  public void readEnvWithoutAnInjectedPortThrowsRuntimeFailure() {
    List<String> output = new ArrayList<>();
    var interpreter = new Interpreter(output::add, ArithmeticOperators.v1());
    var statements = Execution.validatedV11("let count: number = readEnv(\"COUNT\");");

    assertThrows(
        DiagnosticException.class,
        () -> Execution.run(interpreter, statements),
        "expected readEnv to fail as a structured DiagnosticException without an injected environment port");
  }

  @Test
  public void arithmeticOperatorsAdd() {
    assertEquals(
        new BigDecimal("5"),
        ArithmeticOperators.v1()
            .apply(BinaryOperator.ADD, new BigDecimal("3"), new BigDecimal("2")),
        "expected addition");
  }

  @Test
  public void arithmeticOperatorsSubtract() {
    assertEquals(
        new BigDecimal("1"),
        ArithmeticOperators.v1()
            .apply(BinaryOperator.SUBTRACT, new BigDecimal("3"), new BigDecimal("2")),
        "expected subtraction");
  }

  @Test
  public void arithmeticOperatorsMultiply() {
    assertEquals(
        new BigDecimal("6"),
        ArithmeticOperators.v1()
            .apply(BinaryOperator.MULTIPLY, new BigDecimal("3"), new BigDecimal("2")),
        "expected multiplication");
  }

  @Test
  public void arithmeticOperatorsDivide() {
    assertEquals(
        new BigDecimal("1.5"),
        ArithmeticOperators.v1()
            .apply(BinaryOperator.DIVIDE, new BigDecimal("3"), new BigDecimal("2")),
        "expected division");
  }

  @Test
  public void arithmeticOperatorsRejectDivisionByZero() {
    ArithmeticOperators operators = ArithmeticOperators.v1();

    assertThrows(
        ArithmeticException.class,
        () -> operators.apply(BinaryOperator.DIVIDE, BigDecimal.ONE, BigDecimal.ZERO));
  }

  @Test
  public void numberValueReportsTheNumberTypeName() {
    assertEquals(
        org.printscript.types.TypeName.NUMBER,
        new RuntimeValue.NumberValue(BigDecimal.ONE).type(),
        "expected the number type name");
  }

  @Test
  public void stringValueReportsTheStringTypeName() {
    assertEquals(
        org.printscript.types.TypeName.STRING,
        new RuntimeValue.StringValue("x").type(),
        "expected the string type name");
  }

  @Test
  public void booleanValueReportsTheBooleanTypeName() {
    assertEquals(
        org.printscript.types.TypeName.BOOLEAN,
        new RuntimeValue.BooleanValue(true).type(),
        "expected the boolean type name");
  }

  @Test
  public void unitValueReportsNoTypeName() {
    assertEquals(null, RuntimeValue.UnitValue.INSTANCE.type(), "expected no type name for unit");
  }
}
