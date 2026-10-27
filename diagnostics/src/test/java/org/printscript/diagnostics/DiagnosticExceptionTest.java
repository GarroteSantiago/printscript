package org.printscript.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.source.SourcePosition;
import org.printscript.source.SourceSpan;

class DiagnosticExceptionTest {
  private static final SourceSpan SPAN = SourceSpan.at(new SourcePosition(1, 1, 0));
  private static final Diagnostic FIRST = Diagnostic.error(Phase.SYNTAX, "unexpected token", SPAN);
  private static final Diagnostic SECOND = Diagnostic.error(Phase.SEMANTIC, "unknown name", SPAN);

  @Test
  public void singleDiagnosticConstructorCarriesThatDiagnostic() {
    assertEquals(
        List.of(FIRST),
        new DiagnosticException(FIRST).diagnostics(),
        "expected the given diagnostic");
  }

  @Test
  public void listConstructorCarriesEveryDiagnostic() {
    assertEquals(
        List.of(FIRST, SECOND),
        new DiagnosticException(List.of(FIRST, SECOND)).diagnostics(),
        "expected every given diagnostic, in order");
  }

  @Test
  public void messageIsTheFirstDiagnosticsMessage() {
    assertEquals(
        "unexpected token",
        new DiagnosticException(List.of(FIRST, SECOND)).getMessage(),
        "expected the first diagnostic's message");
  }

  @Test
  public void diagnosticsAreACopyOfTheGivenList() {
    List<Diagnostic> given = new ArrayList<>(List.of(FIRST));
    DiagnosticException exception = new DiagnosticException(given);
    given.add(SECOND);

    assertEquals(List.of(FIRST), exception.diagnostics(), "expected later changes to be ignored");
  }

  @Test
  public void rejectsAnEmptyListOfDiagnostics() {
    assertThrows(IllegalArgumentException.class, () -> new DiagnosticException(List.of()));
  }
}
