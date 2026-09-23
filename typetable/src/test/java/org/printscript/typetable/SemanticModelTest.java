package org.printscript.typetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.expressions.CallExpressionSyntax;
import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.ast.nodes.expressions.IdentifierExpressionSyntax;
import org.printscript.ast.nodes.statements.ExpressionStatementSyntax;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.diagnostics.Phase;
import org.printscript.testkit.TestSources;
import org.printscript.types.TypeName;

class SemanticModelTest {
  private VariableDeclarationSyntax declaration() {
    return (VariableDeclarationSyntax) TestSources.statementsOf("let x: number = 1;").next();
  }

  private CallExpressionSyntax call() {
    var statement = (ExpressionStatementSyntax) TestSources.statementsOf("println(x);").next();
    return (CallExpressionSyntax) statement.expression();
  }

  private IdentifierExpressionSyntax identifier() {
    return (IdentifierExpressionSyntax) call().arguments().getFirst();
  }

  @Test
  void typeOfReturnsTheResolvedTypeForATypedExpression() {
    ExpressionSyntax literal = declaration().initializer().orElseThrow();
    var builder = SemanticModel.builder();
    builder.setType(literal, TypeName.NUMBER);

    assertEquals(
        Optional.of(TypeName.NUMBER),
        builder.build().typeOf(literal),
        "expected the resolved type to come back for the expression it was recorded against");
  }

  @Test
  void typeOfReturnsEmptyForAnExpressionNeverRecorded() {
    ExpressionSyntax literal = declaration().initializer().orElseThrow();

    assertTrue(
        SemanticModel.builder().build().typeOf(literal).isEmpty(),
        "expected no resolution for an expression the builder never saw");
  }

  @Test
  void resolveVariableReturnsTheResolvedSymbol() {
    IdentifierExpressionSyntax identifier = identifier();
    VariableSymbol symbol = new VariableSymbol("x", TypeName.NUMBER, false, declaration());
    var builder = SemanticModel.builder();
    builder.resolveVariable(identifier, symbol);

    assertEquals(
        Optional.of(symbol),
        builder.build().resolveVariable(identifier),
        "expected the identifier to resolve to the recorded symbol");
  }

  @Test
  void resolveVariableReturnsEmptyForAnUnresolvedIdentifier() {
    assertTrue(
        SemanticModel.builder().build().resolveVariable(identifier()).isEmpty(),
        "expected no resolution for an identifier the builder never saw");
  }

  @Test
  void resolveCallReturnsTheResolvedSignature() {
    CallExpressionSyntax call = call();
    BuiltinSignature signature = BuiltinRegistry.v1().find(BuiltinRegistry.PRINTLN).orElseThrow();
    var builder = SemanticModel.builder();
    builder.resolveCall(call, signature);

    assertEquals(
        Optional.of(signature),
        builder.build().resolveCall(call),
        "expected the call to resolve to the recorded builtin signature");
  }

  @Test
  void diagnosticsIncludesEveryAddedDiagnostic() {
    Diagnostic diagnostic =
        Diagnostic.error(Phase.SEMANTIC, "undeclared variable", identifier().span());
    var builder = SemanticModel.builder();
    builder.addDiagnostic(diagnostic);

    assertEquals(
        java.util.List.of(diagnostic),
        builder.build().diagnostics(),
        "expected every diagnostic added to the builder to survive into the built model");
  }

  @Test
  void builderHasErrorsIsFalseWithNoDiagnostics() {
    assertFalse(
        SemanticModel.builder().hasErrors(), "expected a fresh builder to report no errors");
  }

  @Test
  void builderHasErrorsIsTrueAfterAddingADiagnostic() {
    var builder = SemanticModel.builder();
    builder.addDiagnostic(
        Diagnostic.error(Phase.SEMANTIC, "undeclared variable", identifier().span()));

    assertTrue(builder.hasErrors(), "expected hasErrors() to reflect an added diagnostic");
  }
}
