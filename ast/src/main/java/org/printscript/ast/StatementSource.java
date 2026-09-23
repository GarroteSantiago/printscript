package org.printscript.ast;

import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.tokens.SyntaxToken;

/**
 * Pull-based port for a stream of parsed statements, analogous to {@link
 * org.printscript.tokens.TokenSource} one layer up. {@code hasNext()}/{@code next()} let a consumer
 * (the {@code application} composition root, or a test) process one statement at a time without
 * materializing a full {@link org.printscript.ast.nodes.ProgramSyntax}; {@link #eof()} exposes the
 * trailing end-of-file token once the stream is exhausted, mainly so its trivia can still be
 * recovered for formatting. {@code parser.StatementSyntaxReader} is the sole production
 * implementation.
 */
public interface StatementSource {
  boolean hasNext();

  StatementSyntax next();

  SyntaxToken eof();
}
