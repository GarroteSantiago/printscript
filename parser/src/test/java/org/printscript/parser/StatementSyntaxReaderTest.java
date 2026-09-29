package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.printscript.testkit.TestSources;
import org.printscript.tokens.SyntaxException;

class StatementSyntaxReaderTest {
  private SyntaxException expectSyntaxException(Executable executable, String failureMessage) {
    return assertThrows(SyntaxException.class, executable, failureMessage);
  }

  @Test
  public void nextThrowsWhenNoStatementsRemain() {
    var statements = TestSources.statementsOf("");

    SyntaxException exception =
        expectSyntaxException(statements::next, "expected calling next() at EOF to fail");

    assertEquals(
        "Expected statement", exception.getMessage(), "expected the 'no statements left' message");
  }

  @Test
  public void primaryThrowsWhenNoExpressionMatches() {
    SyntaxException exception =
        expectSyntaxException(
            () -> TestSources.programOf(";"),
            "expected a lone ';' to fail to parse as an expression statement");

    assertEquals(
        "Expected expression",
        exception.getMessage(),
        "expected the 'no expression matched' message");
  }
}
