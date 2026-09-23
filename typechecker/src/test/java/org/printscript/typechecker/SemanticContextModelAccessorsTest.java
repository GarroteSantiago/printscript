package org.printscript.typechecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.expressions.BinaryExpressionSyntax;
import org.printscript.ast.nodes.expressions.CallExpressionSyntax;
import org.printscript.ast.nodes.expressions.IdentifierExpressionSyntax;
import org.printscript.ast.nodes.statements.ExpressionStatementSyntax;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;
import org.printscript.types.TypeAnnotationTable;
import org.printscript.types.TypeName;
import org.printscript.typetable.BuiltinRegistry;
import org.printscript.typetable.BuiltinSignature;
import org.printscript.typetable.SemanticModel;
import org.printscript.typetable.VariableSymbol;

class SemanticContextModelAccessorsTest {
  @Test
  void resolvesAnIdentifierReferenceToItsDeclaringSymbol() {
    var declaration =
        (VariableDeclarationSyntax) TestSources.statementsOf("let x: number = 1;").next();
    var reference = (ExpressionStatementSyntax) TestSources.statementsOf("println(x);").next();
    var call = (CallExpressionSyntax) reference.expression();
    var identifier = (IdentifierExpressionSyntax) call.arguments().getFirst();

    var context = SemanticContext.empty(BuiltinRegistry.v1());
    var declarationResult = context.validate(declaration);
    var referenceResult = declarationResult.nextContext().validate(reference);

    assertEquals(
        Optional.of("x"),
        referenceResult.semanticModel().resolveVariable(identifier).map(VariableSymbol::name),
        "expected the identifier to resolve to the 'x' symbol");
  }

  @Test
  void resolvesACallExpressionToItsBuiltinSignature() {
    var statement = (ExpressionStatementSyntax) TestSources.statementsOf("println(\"hi\");").next();
    var call = (CallExpressionSyntax) statement.expression();

    var result = SemanticContext.empty(BuiltinRegistry.v1()).validate(statement);

    assertEquals(
        Optional.of("println"),
        result.semanticModel().resolveCall(call).map(BuiltinSignature::name),
        "expected the call to resolve to the println builtin");
  }

  @Test
  void tracksTheInferredTypeOfABinaryExpression() {
    var declaration =
        (VariableDeclarationSyntax) TestSources.statementsOf("let x: number = 1 + 2;").next();
    var binary = (BinaryExpressionSyntax) declaration.initializer().orElseThrow();

    var result = SemanticContext.empty(BuiltinRegistry.v1()).validate(declaration);

    assertEquals(
        Optional.of(TypeName.NUMBER),
        result.semanticModel().typeOf(binary),
        "expected the binary expression to be typed as number");
  }

  @Test
  void rejectsBinaryOperandsThatCannotBeCombined() {
    var model = buildMismatchedBinaryOperandsModel();

    assertEquals(1, model.diagnostics().size(), "expected one diagnostic");
  }

  @Test
  void reportsAnOperatorNotApplicableMessageForMismatchedOperands() {
    var model = buildMismatchedBinaryOperandsModel();

    assertTrue(
        model.diagnostics().getFirst().message().contains("cannot be applied"),
        "expected an operator-not-applicable diagnostic");
  }

  private SemanticModel buildMismatchedBinaryOperandsModel() {
    var program =
        TestSources.programOf(
            """
            let flag: boolean = true;
            let result: number = flag + 1;
            """,
            KeywordTable.v1_1());

    return new SemanticModelBuilder(BuiltinRegistry.v1_1(), TypeAnnotationTable.v1_1())
        .build(program);
  }
}
