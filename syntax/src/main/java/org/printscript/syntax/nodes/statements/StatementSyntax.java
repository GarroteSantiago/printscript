package org.printscript.syntax.nodes.statements;

import org.printscript.syntax.nodes.SyntaxNode;

/**
 * A statement AST node (declaration, assignment, expression statement, {@code if}, or block).
 * {@code non-sealed} so it stays open for new statement kinds while {@link SyntaxNode} itself stays
 * closed. Every consumer (semantics, interpreter, formatter, analyzer) dispatches on the concrete
 * kind through {@link #accept}/{@link StatementVisitor}, never {@code instanceof}.
 */
public non-sealed interface StatementSyntax extends SyntaxNode {
  <R> R accept(StatementVisitor<R> visitor);
}
