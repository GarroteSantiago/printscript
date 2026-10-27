package org.printscript.typechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.statements.BlockStatementSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;

/** The error paths of {@link SemanticContext}: each test checks the one diagnostic it reports. */
class SemanticContextErrorsTest {
  private static String firstMessage(List<Diagnostic> diagnostics) {
    return diagnostics.stream().map(Diagnostic::message).findFirst().orElse("<none>");
  }

  private String firstMessage(String source) {
    return firstMessage(Validation.diagnosticsOf(source));
  }

  private String firstMessageV11(String source) {
    return firstMessage(Validation.diagnosticsOfV11(source));
  }

  @Test
  public void rejectsRedeclaringAVariable() {
    assertEquals(
        "Variable 'x' is already declared",
        firstMessage("let x: number = 1;\nlet x: number = 2;"),
        "expected a redeclaration diagnostic");
  }

  @Test
  public void rejectsAssigningToAnUndeclaredVariable() {
    assertEquals(
        "Variable 'y' is not declared",
        firstMessage("y = 1;"),
        "expected an undeclared-variable diagnostic");
  }

  @Test
  public void rejectsAssigningAValueOfTheWrongType() {
    assertEquals(
        "Cannot assign string to number",
        firstMessage("let x: number = 1;\nx = \"text\";"),
        "expected a type-mismatch diagnostic on assignment");
  }

  @Test
  public void rejectsCallingAnUnknownCallable() {
    assertEquals(
        "Unknown callable 'foo'",
        firstMessage("foo(1);"),
        "expected an unknown-callable diagnostic");
  }

  @Test
  public void rejectsCallingWithTheWrongNumberOfArguments() {
    assertEquals(
        "Callable 'println' expects 1 argument(s)",
        firstMessage("println();"),
        "expected an argument-count diagnostic");
  }

  @Test
  public void rejectsAnArgumentOfTheWrongType() {
    assertEquals(
        "Callable 'readEnv' expects string but received number",
        firstMessageV11("let s: string = readEnv(1);"),
        "expected an argument-type diagnostic");
  }

  @Test
  public void validatesTheStatementsOfABlockPassedAsAStatement() {
    var inner = TestSources.statementsOf("println(missing);").next();
    var block = new BlockStatementSyntax(List.of(inner), inner.span());

    assertEquals(
        List.of("Variable 'missing' is not declared"),
        SemanticContext.forVersion(true).validate(block).diagnostics().stream()
            .map(Diagnostic::message)
            .toList(),
        "expected the statements inside a block to be validated");
  }

  @Test
  public void forVersion11AcceptsV11Builtins() {
    var statement =
        TestSources.statementsOf("let s: string = readEnv(\"HOME\");", KeywordTable.v1_1()).next();

    assertTrue(
        SemanticContext.forVersion(true).validate(statement).isSuccess(),
        "expected readEnv to be known under v1.1");
  }

  @Test
  public void forVersion10RejectsV11Builtins() {
    var statement = TestSources.statementsOf("let s: string = readEnv(\"HOME\");").next();

    assertEquals(
        List.of("Unknown callable 'readEnv'"),
        SemanticContext.forVersion(false).validate(statement).diagnostics().stream()
            .map(Diagnostic::message)
            .toList(),
        "expected readEnv to be unknown under v1.0");
  }
}
