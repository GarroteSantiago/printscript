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
    assertEquals(Phase.ANALYZER, Phase.valueOf("ANALYZER"), "expected the analyzer phase");
  }

  @Test
  public void errorFactoryCarriesNoSpecificCode() {
    assertEquals(
        DiagnosticCode.UNSPECIFIED,
        Diagnostic.error(Phase.SYNTAX, MESSAGE, SPAN).code(),
        "expected an unspecified code by default");
  }

  @Test
  public void withCodeReplacesTheCode() {
    Diagnostic diagnostic =
        Diagnostic.error(Phase.SYNTAX, MESSAGE, SPAN)
            .withCode(DiagnosticCode.UNEXPECTED_END_OF_INPUT);

    assertEquals(
        DiagnosticCode.UNEXPECTED_END_OF_INPUT, diagnostic.code(), "expected the new code");
  }

  @Test
  public void withCodeKeepsEverythingElse() {
    Diagnostic original = Diagnostic.error(Phase.SYNTAX, MESSAGE, SPAN);

    assertEquals(
        original,
        original
            .withCode(DiagnosticCode.UNEXPECTED_END_OF_INPUT)
            .withCode(DiagnosticCode.UNSPECIFIED),
        "expected severity, phase, message, and span to be unchanged");
  }
}
