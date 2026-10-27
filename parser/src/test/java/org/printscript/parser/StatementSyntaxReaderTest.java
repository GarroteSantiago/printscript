package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.printscript.diagnostics.DiagnosticCode;
import org.printscript.diagnostics.DiagnosticException;
import org.printscript.testkit.TestSources;

class StatementSyntaxReaderTest {
  private DiagnosticException expectSyntaxException(Executable executable, String failureMessage) {
    return assertThrows(DiagnosticException.class, executable, failureMessage);
  }

  @Test
  public void nextThrowsWhenNoStatementsRemain() {
    var statements = TestSources.statementsOf("");

    DiagnosticException exception =
        expectSyntaxException(statements::next, "expected calling next() at EOF to fail");

    assertEquals(
        "Expected statement", exception.getMessage(), "expected the 'no statements left' message");
  }

  @Test
  public void primaryThrowsWhenNoExpressionMatches() {
    DiagnosticException exception =
        expectSyntaxException(
            () -> TestSources.statementListOf(";"),
            "expected a lone ';' to fail to parse as an expression statement");

    assertEquals(
        "Expected expression",
        exception.getMessage(),
        "expected the 'no expression matched' message");
  }

  private DiagnosticCode codeOfParseFailure(String source) {
    return expectSyntaxException(
            () -> TestSources.statementListOf(source), "expected a syntax error")
        .diagnostics()
        .getFirst()
        .code();
  }

  @Test
  public void aMissingSemicolonAtTheEndIsMarkedAsUnexpectedEndOfInput() {
    assertEquals(
        DiagnosticCode.UNEXPECTED_END_OF_INPUT,
        codeOfParseFailure("let x: number = 1"),
        "expected input that ends mid-statement to be marked as incomplete");
  }

  @Test
  public void aTrailingOperatorIsMarkedAsUnexpectedEndOfInput() {
    assertEquals(
        DiagnosticCode.UNEXPECTED_END_OF_INPUT,
        codeOfParseFailure("let x: number = 1 +"),
        "expected input that ends after an operator to be marked as incomplete");
  }

  @Test
  public void anErrorBeforeTheEndOfInputHasNoSpecificCode() {
    assertEquals(
        DiagnosticCode.UNSPECIFIED,
        codeOfParseFailure("let x number = 1;"),
        "expected a genuine syntax error not to be marked as incomplete");
  }
}
