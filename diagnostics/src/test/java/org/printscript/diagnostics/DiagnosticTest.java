package org.printscript.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.printscript.source.SourcePosition;
import org.printscript.source.SourceSpan;

class DiagnosticTest {
  private static final SourceSpan SPAN = SourceSpan.at(new SourcePosition(1, 1, 0));
  private static final String MESSAGE = "bad token";

  @Test
  void errorFactoryCreatesADiagnosticWithErrorSeverity() {
    Diagnostic diagnostic = Diagnostic.error(Phase.SYNTAX, MESSAGE, SPAN);

    assertEquals(Severity.ERROR, diagnostic.severity(), "expected an error severity");
  }

  @Test
  void errorFactoryPreservesThePhase() {
    Diagnostic diagnostic = Diagnostic.error(Phase.SEMANTIC, MESSAGE, SPAN);

    assertEquals(Phase.SEMANTIC, diagnostic.phase(), "expected the given phase");
  }

  @Test
  void errorFactoryPreservesTheMessage() {
    Diagnostic diagnostic = Diagnostic.error(Phase.RUNTIME, MESSAGE, SPAN);

    assertEquals(MESSAGE, diagnostic.message(), "expected the given message");
  }

  @Test
  void errorFactoryPreservesTheSpan() {
    Diagnostic diagnostic = Diagnostic.error(Phase.FORMATTER, MESSAGE, SPAN);

    assertEquals(SPAN, diagnostic.span(), "expected the given span");
  }

  @Test
  void warningSeverityIsDistinctFromError() {
    assertEquals(2, Severity.values().length, "expected exactly two severities");
  }

  @Test
  void everyPhaseIsResolvableByName() {
    assertEquals(Phase.APPLICATION, Phase.valueOf("APPLICATION"), "expected the application phase");
  }
}
