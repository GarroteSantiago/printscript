package org.printscript.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.diagnostics.Phase;
import org.printscript.source.SourcePosition;
import org.printscript.source.SourceSpan;

class SyntaxExceptionTest {
  private static final Diagnostic DIAGNOSTIC =
      Diagnostic.error(
          Phase.SYNTAX, "unexpected token", SourceSpan.at(new SourcePosition(1, 1, 0)));

  @Test
  public void diagnosticReturnsTheGivenDiagnostic() {
    SyntaxException exception = new SyntaxException(DIAGNOSTIC);

    assertEquals(DIAGNOSTIC, exception.diagnostic(), "expected the given diagnostic");
  }

  @Test
  public void messageMatchesTheDiagnosticMessage() {
    SyntaxException exception = new SyntaxException(DIAGNOSTIC);

    assertEquals("unexpected token", exception.getMessage(), "expected the diagnostic's message");
  }
}
