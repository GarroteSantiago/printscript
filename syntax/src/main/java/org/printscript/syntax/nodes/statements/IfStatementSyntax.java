package org.printscript.syntax.nodes.statements;

import java.util.Optional;
import org.printscript.source.SourceSpan;
import org.printscript.syntax.nodes.expressions.ExpressionSyntax;
import org.printscript.tokens.SyntaxToken;

public record IfStatementSyntax(
    SyntaxToken ifKeyword,
    SyntaxToken leftParen,
    ExpressionSyntax condition,
    SyntaxToken rightParen,
    BlockStatementSyntax thenBlock,
    Optional<SyntaxToken> elseKeyword,
    Optional<BlockStatementSyntax> elseBlock)
    implements StatementSyntax {
  @Override
  public SourceSpan span() {
    SourceSpan end = elseBlock.map(BlockStatementSyntax::span).orElse(thenBlock.span());
    return new SourceSpan(ifKeyword.span().start(), end.end());
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visitIf(this);
  }
}
