package org.printscript.syntax.nodes.expressions;

import org.printscript.syntax.nodes.SyntaxNode;

public non-sealed interface ExpressionSyntax extends SyntaxNode {
  <R> R accept(ExpressionVisitor<R> visitor);
}
