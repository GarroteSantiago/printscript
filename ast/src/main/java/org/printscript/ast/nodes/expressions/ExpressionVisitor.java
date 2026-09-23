package org.printscript.ast.nodes.expressions;

public interface ExpressionVisitor<R> {
  R visitLiteral(LiteralExpressionSyntax literal);

  R visitIdentifier(IdentifierExpressionSyntax identifier);

  R visitBinary(BinaryExpressionSyntax binary);

  R visitCall(CallExpressionSyntax call);
}
