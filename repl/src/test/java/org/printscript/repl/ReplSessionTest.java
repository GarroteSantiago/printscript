package org.printscript.repl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.StatementSource;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.diagnostics.Phase;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;

class ReplSessionTest {
  @Test
  public void stepOnAWellFormedDeclarationProducesNoDiagnostics() {
    List<String> output = new ArrayList<>();
    ReplSession session = new ReplSession(false, output::add);
    StatementSource statements = TestSources.statementsOf("let x: number = 1;");

    List<Diagnostic> diagnostics = session.step(statements);

    assertTrue(diagnostics.isEmpty(), "expected no diagnostics for a valid declaration");
  }

  private record VariablePersistenceResult(
      List<Diagnostic> first, List<Diagnostic> second, List<String> output) {}

  private VariablePersistenceResult runDeclarationThenReference() {
    List<String> output = new ArrayList<>();
    ReplSession session = new ReplSession(false, output::add);
    StatementSource statements = TestSources.statementsOf("let x: number = 5; println(x + 1);");

    List<Diagnostic> first = session.step(statements);
    List<Diagnostic> second = session.step(statements);

    return new VariablePersistenceResult(first, second, output);
  }

  @Test
  public void declarationStepSucceeds() {
    assertTrue(
        runDeclarationThenReference().first().isEmpty(), "expected the declaration to succeed");
  }

  @Test
  public void stepReferencingAnEarlierVariableSucceeds() {
    assertTrue(
        runDeclarationThenReference().second().isEmpty(),
        "expected the reference to the earlier variable to succeed");
  }

  @Test
  public void stepReferencingAnEarlierVariableSeesItsPersistedValue() {
    assertEquals(
        List.of("6"),
        runDeclarationThenReference().output(),
        "expected println to see the persisted value of x");
  }

  private record SemanticErrorResult(
      List<Diagnostic> failed,
      List<Diagnostic> recovered,
      List<Diagnostic> third,
      List<String> output) {}

  private SemanticErrorResult runSemanticErrorThenRecovery() {
    List<String> output = new ArrayList<>();
    ReplSession session = new ReplSession(false, output::add);
    StatementSource statements =
        TestSources.statementsOf("let x: string = 1; let y: number = 2; println(y);");

    List<Diagnostic> failed = session.step(statements);
    List<Diagnostic> recovered = session.step(statements);
    List<Diagnostic> third = session.step(statements);

    return new SemanticErrorResult(failed, recovered, third, output);
  }

  @Test
  public void semanticErrorReportsADiagnostic() {
    assertFalse(
        runSemanticErrorThenRecovery().failed().isEmpty(), "expected a type-mismatch diagnostic");
  }

  @Test
  public void semanticErrorReportsASemanticPhaseDiagnostic() {
    assertEquals(
        Phase.SEMANTIC,
        runSemanticErrorThenRecovery().failed().get(0).phase(),
        "expected a semantic-phase diagnostic");
  }

  @Test
  public void theStatementAfterASemanticErrorValidatesIndependently() {
    assertTrue(
        runSemanticErrorThenRecovery().recovered().isEmpty(),
        "expected the next statement to validate independently");
  }

  @Test
  public void referenceToTheSuccessfullyDeclaredVariableAfterTheErrorSucceeds() {
    assertTrue(
        runSemanticErrorThenRecovery().third().isEmpty(),
        "expected println(y) to see the successfully declared y");
  }

  @Test
  public void failedDeclarationContributesNoOutput() {
    assertEquals(
        List.of("2"),
        runSemanticErrorThenRecovery().output(),
        "expected the failed declaration to contribute nothing");
  }

  private record RuntimeFailureResult(
      List<Diagnostic> declared,
      List<Diagnostic> failed,
      List<Diagnostic> after,
      List<String> output) {}

  private RuntimeFailureResult runRuntimeFailureThenRecovery() {
    List<String> output = new ArrayList<>();
    ReplSession session = new ReplSession(false, output::add);
    StatementSource statements =
        TestSources.statementsOf("let x: number = 10; let y: number = 1 / 0; println(x);");

    List<Diagnostic> declared = session.step(statements);
    List<Diagnostic> failed = session.step(statements);
    List<Diagnostic> after = session.step(statements);

    return new RuntimeFailureResult(declared, failed, after, output);
  }

  @Test
  public void theFirstDeclarationBeforeARuntimeFailureSucceeds() {
    assertTrue(
        runRuntimeFailureThenRecovery().declared().isEmpty(),
        "expected the first declaration to succeed");
  }

  @Test
  public void runtimeFailureReportsADiagnostic() {
    assertFalse(
        runRuntimeFailureThenRecovery().failed().isEmpty(),
        "expected a division-by-zero diagnostic");
  }

  @Test
  public void runtimeFailureReportsARuntimePhaseDiagnostic() {
    assertEquals(
        Phase.RUNTIME,
        runRuntimeFailureThenRecovery().failed().get(0).phase(),
        "expected a runtime-phase diagnostic");
  }

  @Test
  public void variableDeclaredBeforeARuntimeFailureIsStillVisibleAfterwards() {
    assertTrue(
        runRuntimeFailureThenRecovery().after().isEmpty(),
        "expected x, declared before the failure, to still be visible");
  }

  @Test
  public void failedRuntimeStatementPrintsNothing() {
    assertEquals(
        List.of("10"),
        runRuntimeFailureThenRecovery().output(),
        "expected the failed statement to print nothing");
  }

  private record BlockStatementResult(List<Diagnostic> diagnostics, List<String> output) {}

  private BlockStatementResult runV11BlockStatement() {
    List<String> output = new ArrayList<>();
    ReplSession session = new ReplSession(true, output::add);
    StatementSource statements =
        TestSources.statementsOf("if (true) { println(\"yes\"); }", KeywordTable.v1_1());

    List<Diagnostic> diagnostics = session.step(statements);

    return new BlockStatementResult(diagnostics, output);
  }

  @Test
  public void v11IfBlockValidatesAndExecutesWithoutDiagnostics() {
    assertTrue(
        runV11BlockStatement().diagnostics().isEmpty(),
        "expected the v1.1 if-block to validate and execute");
  }

  @Test
  public void v11IfBlockRunsItsBody() {
    assertEquals(
        List.of("yes"), runV11BlockStatement().output(), "expected the block body to execute");
  }
}
