package org.printscript.tokens;

/**
 * Pull-based port for a stream of tokens: call {@link #next()} until it keeps returning {@link
 * TokenType#EOF}. This is the seam that lets {@code lexer.Lexer} (the only production
 * implementation) and {@code syntax.StatementSyntaxReader} (the only production consumer) each
 * depend only on {@code tokens}, without depending on each other — a stage should type against this
 * interface, never against a concrete {@code Lexer}, except at the composition root that constructs
 * one.
 */
public interface TokenSource {
  Token next();
}
