package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.diagnostics.Phase;
import org.printscript.diagnostics.Severity;
import org.printscript.lexer.KeywordTable;
import org.printscript.semantics.BuiltinRegistry;
import org.printscript.semantics.SemanticContext;
import org.printscript.semantics.SemanticStatementResult;
import org.printscript.syntax.TypeAnnotationTable;
import org.printscript.syntax.nodes.statements.StatementSyntax;
import org.printscript.testkit.TestSources;
import org.printscript.tokens.TokenType;

class InterpreterRuntimeFailureTest {
  private static final InputPort FAILING_INPUT =
      prompt -> {
        throw new AssertionError("unexpected read from stdin");
      };
  private static final EnvironmentPort EMPTY_ENVIRONMENT = name -> Optional.empty();

  private RuntimeFailure runAndCaptureFailure(String source, InputPort input, EnvironmentPort env) {
    return assertThrows(
        RuntimeFailure.class,
        () -> {
          List<String> output = new ArrayList<>();
          var statements = TestSources.statementsOf(source);
          var semanticContext = SemanticContext.empty(BuiltinRegistry.v1());
          var interpreter = new Interpreter(output::add, ArithmeticOperators.v1(), input, env);
          var environment = RuntimeEnvironment.empty();
          while (statements.hasNext()) {
            StatementSyntax statement = statements.next();
            SemanticStatementResult semantic = semanticContext.validate(statement);
            environment =
                interpreter.executeStatement(statement, environment, semantic.semanticModel());
            semanticContext = semantic.nextContext();
          }
        },
        "expected execution to fail");
  }

  @Test
  void divisionByZeroThrowsARuntimeFailure() {
    RuntimeFailure failure =
        runAndCaptureFailure("let result: number = 1 / 0;", FAILING_INPUT, EMPTY_ENVIRONMENT);

    assertEquals("Division by zero", failure.getMessage(), "expected a division-by-zero message");
  }

  @Test
  void divisionByZeroDiagnosticHasErrorSeverity() {
    RuntimeFailure failure =
        runAndCaptureFailure("let result: number = 1 / 0;", FAILING_INPUT, EMPTY_ENVIRONMENT);

    assertEquals(Severity.ERROR, failure.diagnostic().severity(), "expected an error severity");
  }

  @Test
  void divisionByZeroDiagnosticIsFromTheRuntimePhase() {
    RuntimeFailure failure =
        runAndCaptureFailure("let result: number = 1 / 0;", FAILING_INPUT, EMPTY_ENVIRONMENT);

    assertEquals(Phase.RUNTIME, failure.diagnostic().phase(), "expected the runtime phase");
  }

  @Test
  void readInputFailsOnUnparseableNumber() {
    RuntimeFailure failure = runV11AndCaptureFailure("let count: number = readInput(\"prompt\");");

    assertEquals(
        "Expected a number but got 'not-a-number'",
        failure.getMessage(),
        "expected an unparseable-number failure message");
  }

  private RuntimeFailure runV11AndCaptureFailure(String source) {
    return assertThrows(
        RuntimeFailure.class,
        () -> {
          List<String> output = new ArrayList<>();
          var statements = TestSources.statementsOf(source, KeywordTable.v1_1());
          var semanticContext =
              SemanticContext.empty(BuiltinRegistry.v1_1(), TypeAnnotationTable.v1_1());
          var interpreter =
              new Interpreter(
                  output::add,
                  ArithmeticOperators.v1(),
                  prompt -> "not-a-number",
                  EMPTY_ENVIRONMENT);
          var environment = RuntimeEnvironment.empty();
          while (statements.hasNext()) {
            StatementSyntax statement = statements.next();
            SemanticStatementResult semantic = semanticContext.validate(statement);
            environment =
                interpreter.executeStatement(statement, environment, semantic.semanticModel());
            semanticContext = semantic.nextContext();
          }
        },
        "expected execution to fail");
  }

  @Test
  void referencingAVariableDeclaredWithoutAnInitializerBeforeItIsAssignedFails() {
    RuntimeFailure failure =
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
  void readInputWithoutAnInjectedPortThrowsRuntimeFailure() {
    List<String> output = new ArrayList<>();
    var interpreter = new Interpreter(output::add, ArithmeticOperators.v1());
    var statements =
        TestSources.statementsOf("let name: string = readInput(\"prompt\");", KeywordTable.v1_1());
    var semanticContext = SemanticContext.empty(BuiltinRegistry.v1_1(), TypeAnnotationTable.v1_1());
    StatementSyntax statement = statements.next();
    SemanticStatementResult semantic = semanticContext.validate(statement);

    assertThrows(
        RuntimeFailure.class,
        () ->
            interpreter.executeStatement(
                statement, RuntimeEnvironment.empty(), semantic.semanticModel()),
        "expected readInput to fail as a structured RuntimeFailure without an injected input port");
  }

  @Test
  void readEnvWithoutAnInjectedPortThrowsRuntimeFailure() {
    List<String> output = new ArrayList<>();
    var interpreter = new Interpreter(output::add);
    var statements =
        TestSources.statementsOf("let count: number = readEnv(\"COUNT\");", KeywordTable.v1_1());
    var semanticContext = SemanticContext.empty(BuiltinRegistry.v1_1(), TypeAnnotationTable.v1_1());
    StatementSyntax statement = statements.next();
    SemanticStatementResult semantic = semanticContext.validate(statement);

    assertThrows(
        RuntimeFailure.class,
        () ->
            interpreter.executeStatement(
                statement, RuntimeEnvironment.empty(), semantic.semanticModel()),
        "expected readEnv to fail as a structured RuntimeFailure without an injected environment port");
  }

  @Test
  void arithmeticOperatorsAdd() {
    assertEquals(
        new BigDecimal("5"),
        ArithmeticOperators.v1().apply(TokenType.PLUS, new BigDecimal("3"), new BigDecimal("2")),
        "expected addition");
  }

  @Test
  void arithmeticOperatorsSubtract() {
    assertEquals(
        new BigDecimal("1"),
        ArithmeticOperators.v1().apply(TokenType.MINUS, new BigDecimal("3"), new BigDecimal("2")),
        "expected subtraction");
  }

  @Test
  void arithmeticOperatorsMultiply() {
    assertEquals(
        new BigDecimal("6"),
        ArithmeticOperators.v1().apply(TokenType.STAR, new BigDecimal("3"), new BigDecimal("2")),
        "expected multiplication");
  }

  @Test
  void arithmeticOperatorsDivide() {
    assertEquals(
        new BigDecimal("1.5"),
        ArithmeticOperators.v1().apply(TokenType.SLASH, new BigDecimal("3"), new BigDecimal("2")),
        "expected division");
  }

  @Test
  void arithmeticOperatorsRejectDivisionByZero() {
    ArithmeticOperators operators = ArithmeticOperators.v1();

    assertThrows(
        ArithmeticException.class,
        () -> operators.apply(TokenType.SLASH, BigDecimal.ONE, BigDecimal.ZERO));
  }

  @Test
  void arithmeticOperatorsRejectUnsupportedOperators() {
    ArithmeticOperators operators = ArithmeticOperators.v1();

    assertThrows(
        IllegalStateException.class,
        () -> operators.apply(TokenType.EQUAL, BigDecimal.ONE, BigDecimal.ONE));
  }

  @Test
  void numberValueReportsTheNumberTypeName() {
    assertEquals(
        org.printscript.syntax.TypeName.NUMBER,
        new RuntimeValue.NumberValue(BigDecimal.ONE).type(),
        "expected the number type name");
  }

  @Test
  void stringValueReportsTheStringTypeName() {
    assertEquals(
        org.printscript.syntax.TypeName.STRING,
        new RuntimeValue.StringValue("x").type(),
        "expected the string type name");
  }

  @Test
  void booleanValueReportsTheBooleanTypeName() {
    assertEquals(
        org.printscript.syntax.TypeName.BOOLEAN,
        new RuntimeValue.BooleanValue(true).type(),
        "expected the boolean type name");
  }

  @Test
  void unitValueReportsNoTypeName() {
    assertEquals(null, RuntimeValue.UnitValue.INSTANCE.type(), "expected no type name for unit");
  }
}
