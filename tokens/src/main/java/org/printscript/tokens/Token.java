package org.printscript.tokens;

import org.printscript.source.SourceSpan;

/**
 * Raw {@link TokenSource} output: {@code semanticLexeme} is the value the parser/semantics reason
 * about (e.g. a string literal's contents), while {@code text} is the exact source spelling
 * (including quotes) and {@code leadingTrivia} is the whitespace/comments consumed before it. See
 * {@link SyntaxToken} for the immutable copy of this shape that AST nodes actually hold.
 */
public record Token(
    TokenType type, String semanticLexeme, String text, String leadingTrivia, SourceSpan span) {}
