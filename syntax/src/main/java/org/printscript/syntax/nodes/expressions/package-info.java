/**
 * Expression AST nodes: {@link org.printscript.syntax.nodes.expressions.LiteralExpressionSyntax},
 * {@link org.printscript.syntax.nodes.expressions.IdentifierExpressionSyntax}, {@link
 * org.printscript.syntax.nodes.expressions.BinaryExpressionSyntax}, and {@link
 * org.printscript.syntax.nodes.expressions.CallExpressionSyntax} (the shape {@code println}, {@code
 * readInput}, and {@code readEnv} calls parse to). Every consumer dispatches on these through
 * {@link org.printscript.syntax.nodes.expressions.ExpressionVisitor}, never {@code instanceof} —
 * start there to see every place a new expression kind would need a case.
 */
package org.printscript.syntax.nodes.expressions;
