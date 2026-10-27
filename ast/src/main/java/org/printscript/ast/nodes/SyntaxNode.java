package org.printscript.ast.nodes;

import org.printscript.ast.nodes.expressions.ExpressionSyntax;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.source.SourceSpan;

/**
 * Root of the AST. Sealed to exactly {@link StatementSyntax} and {@link ExpressionSyntax} — both
 * are themselves declared {@code non-sealed} so each can be extended with new concrete node kinds
 * without reopening this root. Every node knows its own {@link #span()}, computed from its
 * children's spans rather than stored redundantly, so a diagnostic can always point at exactly the
 * source range a node covers.
 */
public sealed interface SyntaxNode permits StatementSyntax, ExpressionSyntax {
  SourceSpan span();
}
