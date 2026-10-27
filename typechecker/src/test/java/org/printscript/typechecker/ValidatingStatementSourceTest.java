package org.printscript.typechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.printscript.diagnostics.DiagnosticException;
import org.printscript.testkit.TestSources;

class ValidatingStatementSourceTest {
  private ValidatingStatementSource validating(String source) {
    return new ValidatingStatementSource(
        TestSources.statementsOf(source), SemanticContext.forVersion(false));
  }

  private DiagnosticException nextFailure(ValidatingStatementSource statements) {
    return assertThrows(
        DiagnosticException.class, statements::next, "expected the invalid statement to throw");
  }

  @Test
  public void threadsDeclarationsFromOneStatementToTheNext() {
    var statements = validating("let x: number = 1;\nprintln(x);");
    statements.next();

    assertTrue(
        statements.next().model().diagnostics().isEmpty(),
        "expected the second statement to validate using the first one's declaration");
  }

  @Test
  public void hasNoNextOnceEveryStatementIsConsumed() {
    var statements = validating("let x: number = 1;");
    statements.next();

    assertFalse(statements.hasNext(), "expected the stream to be exhausted");
  }

  @Test
  public void throwsTheDiagnosticsOfAnInvalidStatement() {
    DiagnosticException exception = nextFailure(validating("let x: number = \"text\";"));

    assertEquals(
        "Cannot assign string to number",
        exception.getMessage(),
        "expected the validation diagnostic to be thrown");
  }
}
