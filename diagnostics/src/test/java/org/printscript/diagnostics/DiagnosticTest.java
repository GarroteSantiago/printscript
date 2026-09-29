package org.printscript.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.printscript.source.SourcePosition;
import org.printscript.source.SourceSpan;

class DiagnosticTest {
  private static final SourceSpan SPAN = SourceSpan.at(new SourcePosition(1, 1, 0));
  private static final String MESSAGE = "bad token";

  @Test
  public void errorFactoryCreatesADiagnosticWithErrorSeverity() {
    Diagnostic diagnostic = Diagnostic.error(Phase.SYNTAX, MESSAGE, SPAN);

    assertEquals(Severity.ERROR, diagnostic.severity(), "expected an error severity");
  }

  @Test
  public void errorFactoryPreservesThePhase() {
    Diagnostic diagnostic = Diagnostic.error(Phase.SEMANTIC, MESSAGE, SPAN);

    assertEquals(Phase.SEMANTIC, diagnostic.phase(), "expected the given phase");
  }

  @Test
  public void errorFactoryPreservesTheMessage() {
    Diagnostic diagnostic = Diagnostic.error(Phase.RUNTIME, MESSAGE, SPAN);

    assertEquals(MESSAGE, diagnostic.message(), "expected the given message");
  }

  @Test
  public void errorFactoryPreservesTheSpan() {
    Diagnostic diagnostic = Diagnostic.error(Phase.FORMATTER, MESSAGE, SPAN);

    assertEquals(SPAN, diagnostic.span(), "expected the given span");
  }

  @Test
  public void warningSeverityIsDistinctFromError() {
    assertEquals(2, Severity.values().length, "expected exactly two severities");
  }

  @Test
  public void everyPhaseIsResolvableByName() {
    assertEquals(Phase.TOOLCHAIN, Phase.valueOf("TOOLCHAIN"), "expected the toolchain phase");
  }
}
