package org.printscript.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.printscript.testkit.TestSources;
import org.printscript.tokens.SyntaxException;

class StatementSyntaxReaderTest {
  @Test
  void nextThrowsWhenNoStatementsRemain() {
    var statements = TestSources.statementsOf("");

    SyntaxException exception =
        assertThrows(
            SyntaxException.class, statements::next, "expected calling next() at EOF to fail");

    assertEquals(
        "Expected statement", exception.getMessage(), "expected the 'no statements left' message");
  }

  @Test
  void primaryThrowsWhenNoExpressionMatches() {
    SyntaxException exception =
        assertThrows(
            SyntaxException.class,
            () -> TestSources.programOf(";"),
            "expected a lone ';' to fail to parse as an expression statement");

    assertEquals(
        "Expected expression",
        exception.getMessage(),
        "expected the 'no expression matched' message");
  }
}
