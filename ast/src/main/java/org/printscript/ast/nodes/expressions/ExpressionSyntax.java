package org.printscript.ast.nodes.expressions;

import org.printscript.ast.nodes.SyntaxNode;

/**
 * An expression AST node (literal, identifier reference, binary operation, or builtin call). {@code
 * non-sealed} so it stays open for new expression kinds while {@link SyntaxNode} itself stays
 * closed. Every consumer (semantics, interpreter, formatter, analyzer) dispatches on the concrete
 * kind through {@link #accept}/{@link ExpressionVisitor}, never {@code instanceof}.
 */
public non-sealed interface ExpressionSyntax extends SyntaxNode {
  <R> R accept(ExpressionVisitor<R> visitor);
}
