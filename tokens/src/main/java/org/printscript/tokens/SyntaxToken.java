package org.printscript.tokens;

import org.printscript.source.SourceSpan;

/**
 * The token shape AST nodes hold directly (same fields as {@link Token}, copied via {@link #from}).
 * Kept as a distinct type from {@code Token} so a {@code TokenSource}'s streaming output and an
 * AST's retained node data can evolve independently, and so the {@code leadingTrivia} it carries
 * remains available for the formatter's lossless rewriting even after parsing has consumed it.
 */
public record SyntaxToken(
    TokenType type, String semanticLexeme, String text, String leadingTrivia, SourceSpan span) {
  public static SyntaxToken from(Token token) {
    return new SyntaxToken(
        token.type(), token.semanticLexeme(), token.text(), token.leadingTrivia(), token.span());
  }
}
