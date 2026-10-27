package org.printscript.typechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.expressions.BinaryExpressionSyntax;
import org.printscript.ast.nodes.expressions.CallExpressionSyntax;
import org.printscript.ast.nodes.expressions.IdentifierExpressionSyntax;
import org.printscript.ast.nodes.statements.ExpressionStatementSyntax;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.testkit.TestSources;
import org.printscript.types.TypeName;
import org.printscript.typetable.BuiltinSignature;
import org.printscript.typetable.VariableSymbol;

class SemanticContextModelAccessorsTest {
  @Test
  public void resolvesAnIdentifierReferenceToItsDeclaringSymbol() {
    var declaration =
        (VariableDeclarationSyntax) TestSources.statementsOf("let x: number = 1;").next();
    var reference = (ExpressionStatementSyntax) TestSources.statementsOf("println(x);").next();
    var call = (CallExpressionSyntax) reference.expression();
    var identifier = (IdentifierExpressionSyntax) call.arguments().getFirst();

    var context = SemanticContext.forVersion(false);
    var declarationResult = context.validate(declaration);
    var referenceResult = declarationResult.nextContext().validate(reference);

    assertEquals(
        Optional.of("x"),
        referenceResult.semanticModel().resolveVariable(identifier).map(VariableSymbol::name),
        "expected the identifier to resolve to the 'x' symbol");
  }

  @Test
  public void resolvesACallExpressionToItsBuiltinSignature() {
    var statement = (ExpressionStatementSyntax) TestSources.statementsOf("println(\"hi\");").next();
    var call = (CallExpressionSyntax) statement.expression();

    var result = SemanticContext.forVersion(false).validate(statement);

    assertEquals(
        Optional.of("println"),
        result.semanticModel().resolveCall(call).map(BuiltinSignature::name),
        "expected the call to resolve to the println builtin");
  }

  @Test
  public void tracksTheInferredTypeOfABinaryExpression() {
    var declaration =
        (VariableDeclarationSyntax) TestSources.statementsOf("let x: number = 1 + 2;").next();
    var binary = (BinaryExpressionSyntax) declaration.initializer().orElseThrow();

    var result = SemanticContext.forVersion(false).validate(declaration);

    assertEquals(
        Optional.of(TypeName.NUMBER),
        result.semanticModel().typeOf(binary),
        "expected the binary expression to be typed as number");
  }

  @Test
  public void rejectsBinaryOperandsThatCannotBeCombined() {
    var diagnostics = mismatchedBinaryOperandsDiagnostics();

    assertEquals(1, diagnostics.size(), "expected one diagnostic");
  }

  @Test
  public void reportsAnOperatorNotApplicableMessageForMismatchedOperands() {
    var diagnostics = mismatchedBinaryOperandsDiagnostics();

    assertTrue(
        diagnostics.getFirst().message().contains("cannot be applied"),
        "expected an operator-not-applicable diagnostic");
  }

  private List<Diagnostic> mismatchedBinaryOperandsDiagnostics() {
    return Validation.diagnosticsOfV11(
        """
            let flag: boolean = true;
            let result: number = flag + 1;
            """);
  }
}
