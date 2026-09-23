/**
 * Expression AST nodes: {@link org.printscript.ast.nodes.expressions.LiteralExpressionSyntax},
 * {@link org.printscript.ast.nodes.expressions.IdentifierExpressionSyntax}, {@link
 * org.printscript.ast.nodes.expressions.BinaryExpressionSyntax}, and {@link
 * org.printscript.ast.nodes.expressions.CallExpressionSyntax} (the shape {@code println}, {@code
 * readInput}, and {@code readEnv} calls parse to). Every consumer dispatches on these through
 * {@link org.printscript.ast.nodes.expressions.ExpressionVisitor}, never {@code instanceof} — start
 * there to see every place a new expression kind would need a case.
 */
package org.printscript.ast.nodes.expressions;
