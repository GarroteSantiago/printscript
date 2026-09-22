package org.printscript.syntax.nodes.statements;

import org.printscript.syntax.nodes.SyntaxNode;

public non-sealed interface StatementSyntax extends SyntaxNode {
  <R> R accept(StatementVisitor<R> visitor);
}
