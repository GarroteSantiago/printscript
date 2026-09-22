package org.printscript.syntax.nodes.expressions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.printscript.syntax.nodes.ProgramSyntax;
import org.printscript.syntax.nodes.statements.AssignmentSyntax;
import org.printscript.syntax.nodes.statements.ExpressionStatementSyntax;
import org.printscript.testkit.TestSources;

class ExpressionSyntaxSpanTest {
  private static final String SOURCE =
      """
      let a: number = 1;
      a = a + 1;
      println(a);
      """;

  private final ProgramSyntax program = TestSources.programOf(SOURCE);

  private BinaryExpressionSyntax binaryFromAssignment() {
    var assignment =
        assertInstanceOf(
            AssignmentSyntax.class, program.statements().get(1), "expected an assignment");
    return assertInstanceOf(
        BinaryExpressionSyntax.class, assignment.value(), "expected a binary expression");
  }

  private IdentifierExpressionSyntax identifierFromBinary() {
    return assertInstanceOf(
        IdentifierExpressionSyntax.class, binaryFromAssignment().left(), "expected an identifier");
  }

  private LiteralExpressionSyntax literalFromBinary() {
    return assertInstanceOf(
        LiteralExpressionSyntax.class, binaryFromAssignment().right(), "expected a literal");
  }

  private CallExpressionSyntax callExpression() {
    var expressionStatement =
        assertInstanceOf(
            ExpressionStatementSyntax.class,
            program.statements().get(2),
            "expected an expression statement");
    return assertInstanceOf(
        CallExpressionSyntax.class, expressionStatement.expression(), "expected a call expression");
  }

  @Test
  void binaryExpressionSpanStartsAtLeftOperand() {
    var binary = binaryFromAssignment();

    assertEquals(
        binary.left().span().start(),
        binary.span().start(),
        "expected span to start at the left operand");
  }

  @Test
  void binaryExpressionSpanEndsAtRightOperand() {
    var binary = binaryFromAssignment();

    assertEquals(
        binary.right().span().end(),
        binary.span().end(),
        "expected span to end at the right operand");
  }

  @Test
  void binaryExpressionAcceptDispatchesToVisitBinary() {
    var binary = binaryFromAssignment();

    assertEquals(
        "binary", binary.accept(NAMING_VISITOR), "expected accept() to dispatch to visitBinary");
  }

  @Test
  void identifierExpressionSpanMatchesItsToken() {
    var identifier = identifierFromBinary();

    assertEquals(
        identifier.identifier().span(), identifier.span(), "expected the identifier's own span");
  }

  @Test
  void literalExpressionSpanMatchesItsToken() {
    var literal = literalFromBinary();

    assertEquals(literal.literal().span(), literal.span(), "expected the literal's own span");
  }

  @Test
  void callExpressionSpanStartsAtCallee() {
    var call = callExpression();

    assertEquals(
        call.callee().span().start(), call.span().start(), "expected span to start at the callee");
  }

  @Test
  void callExpressionSpanEndsAtRightParen() {
    var call = callExpression();

    assertEquals(
        call.rightParen().span().end(),
        call.span().end(),
        "expected span to end at the right parenthesis");
  }

  private static final ExpressionVisitor<String> NAMING_VISITOR =
      new ExpressionVisitor<>() {
        @Override
        public String visitLiteral(LiteralExpressionSyntax literal) {
          return "literal";
        }

        @Override
        public String visitIdentifier(IdentifierExpressionSyntax identifier) {
          return "identifier";
        }

        @Override
        public String visitBinary(BinaryExpressionSyntax binary) {
          return "binary";
        }

        @Override
        public String visitCall(CallExpressionSyntax call) {
          return "call";
        }
      };
}
