package org.printscript.formatter;

import java.io.IOException;
import java.util.Optional;
import org.printscript.tokens.Token;
import org.printscript.tokens.TokenSource;
import org.printscript.tokens.TokenType;

/**
 * Rewrites source from its token stream alone: each token's text is re-emitted byte for byte, and
 * only the leading trivia {@link SpacingRules} has an opinion about is rewritten — comments and
 * untouched whitespace stay exactly as written. This is a targeted trivia rewrite, not a
 * pretty-printer, so it needs neither a parser nor a tree: the little structure it does use (block
 * nesting depth for indentation, and whether the previous top-level statement was a {@code println}
 * call) is tracked from the tokens themselves.
 *
 * <p>Formatting never validates. Its input is any token sequence — whether that sequence is a valid
 * program is for the caller to decide, not for formatting — so syntactically invalid code still
 * formats (best effort: stray braces never drive the indentation depth below zero). Only a lexical
 * error, raised by the {@link TokenSource} itself, stops it.
 */
public final class PrintScriptFormatter {
  private final SpacingRules spacingRules;

  public PrintScriptFormatter() {
    this(SpacingRules.v1());
  }

  public PrintScriptFormatter(SpacingRules spacingRules) {
    this.spacingRules = spacingRules;
  }

  /**
   * Pulls every token from {@code tokens} up to and including EOF, writing the formatted source to
   * {@code out} as it goes — one token at a time, never buffering the whole program.
   */
  public void formatAll(TokenSource tokens, FormatterConfigProvider config, Appendable out)
      throws IOException {
    Session session = new Session(config);
    Token token = tokens.next();
    while (!TokenType.EOF.equals(token.type())) {
      session.format(token, out);
      token = tokens.next();
    }
    out.append(rewriteTrailingTrivia(session.previous, token, config));
  }

  /** {@link #formatAll} into a string. */
  public String format(TokenSource tokens, FormatterConfigProvider config) {
    StringBuilder out = new StringBuilder();
    try {
      formatAll(tokens, config, out);
    } catch (IOException exception) {
      throw new IllegalStateException("StringBuilder append failed", exception);
    }
    return out.toString();
  }

  /**
   * The per-program formatting state. A top-level statement ends at a {@code ;} at depth 0, or at a
   * {@code }} back at depth 0 that isn't followed by {@code else} — which can only be known once
   * the next token arrives, hence {@link #blockMayEndStatement}. A statement is a {@code println}
   * call when it is exactly {@code println(...)} followed by {@code ;}.
   */
  private final class Session {
    private static final int NOT_CLOSED = -1;

    private final FormatterConfigProvider config;
    private Token previous;
    private int depth;

    private boolean inStatement;
    private boolean blockMayEndStatement;
    private boolean lastStatementWasPrintln;
    private boolean currentStatementFollowsPrintln;
    private int statementTokens;
    private boolean expectingPrintlnParen;
    private boolean startsWithPrintlnCall;
    private int parenDepth;
    private int firstCallClosedAt = NOT_CLOSED;

    private Session(FormatterConfigProvider config) {
      this.config = config;
    }

    void format(Token token, Appendable out) throws IOException {
      if (blockMayEndStatement) {
        blockMayEndStatement = false;
        if (!TokenType.ELSE.equals(token.type())) {
          endStatement(false);
        }
      }
      if (!inStatement) {
        startStatement();
      }
      if (TokenType.RIGHT_BRACE.equals(token.type())) {
        depth = Math.max(0, depth - 1);
      }

      write(token, out);

      if (TokenType.LEFT_BRACE.equals(token.type())) {
        depth++;
      }
      trackStatement(token);
      previous = token;
    }

    private void write(Token token, Appendable out) throws IOException {
      String leadingTrivia =
          rewriteLeadingTrivia(previous, token, config, currentStatementFollowsPrintln);
      String trivia = leadingTrivia;
      if (depth > 0 && hasLineBreak(trivia)) {
        int indentDepth = depth;
        trivia =
            config
                .blockIndentSpaces()
                .map(spaces -> reindent(leadingTrivia, indentDepth, spaces))
                .orElse(trivia);
      }
      out.append(trivia);
      out.append(token.text());
    }

    private void trackStatement(Token token) {
      if (statementTokens == 0) {
        expectingPrintlnParen =
            TokenType.IDENTIFIER.equals(token.type()) && "println".equals(token.semanticLexeme());
      } else if (expectingPrintlnParen) {
        startsWithPrintlnCall = TokenType.LEFT_PAREN.equals(token.type());
        expectingPrintlnParen = false;
      }
      statementTokens++;

      if (TokenType.LEFT_PAREN.equals(token.type())) {
        parenDepth++;
      } else if (TokenType.RIGHT_PAREN.equals(token.type())) {
        parenDepth = Math.max(0, parenDepth - 1);
        if (parenDepth == 0 && firstCallClosedAt == NOT_CLOSED) {
          firstCallClosedAt = statementTokens;
        }
      }

      if (depth == 0 && TokenType.SEMICOLON.equals(token.type())) {
        endStatement(startsWithPrintlnCall && firstCallClosedAt == statementTokens - 1);
      } else if (depth == 0 && TokenType.RIGHT_BRACE.equals(token.type())) {
        blockMayEndStatement = true;
      }
    }

    private void startStatement() {
      inStatement = true;
      currentStatementFollowsPrintln = lastStatementWasPrintln;
    }

    private void endStatement(boolean wasPrintln) {
      lastStatementWasPrintln = wasPrintln;
      inStatement = false;
      statementTokens = 0;
      expectingPrintlnParen = false;
      startsWithPrintlnCall = false;
      parenDepth = 0;
      firstCallClosedAt = NOT_CLOSED;
    }
  }

  private String reindent(String trivia, int depth, int blockIndentSpaces) {
    int lastNewline = trivia.lastIndexOf('\n');
    return trivia.substring(0, lastNewline + 1) + " ".repeat(depth * blockIndentSpaces);
  }

  private String rewriteLeadingTrivia(
      Token previous,
      Token token,
      FormatterConfigProvider config,
      boolean previousStatementIsPrintln) {
    if (previous == null) {
      return token.leadingTrivia();
    }
    if (containsComment(token.leadingTrivia())) {
      if (TokenType.SEMICOLON.equals(previous.type())) {
        return config
            .spacesAfterSemicolon()
            .map(
                spaces ->
                    rewriteWhitespaceBeforeFirstComment(token.leadingTrivia(), " ".repeat(spaces)))
            .orElse(token.leadingTrivia());
      }
      return token.leadingTrivia();
    }
    if (previousStatementIsPrintln && TokenType.SEMICOLON.equals(previous.type())) {
      Optional<String> blankLines = config.blankLinesAfterPrintln().map(n -> "\n".repeat(n + 1));
      if (blankLines.isPresent()) {
        return blankLines.get();
      }
    }
    return spacingRules.leadingTriviaFor(token, previous, config).orElse(token.leadingTrivia());
  }

  private String rewriteTrailingTrivia(Token previous, Token eof, FormatterConfigProvider config) {
    if (previous == null || containsComment(eof.leadingTrivia())) {
      return eof.leadingTrivia();
    }
    if (TokenType.SEMICOLON.equals(previous.type()) && !hasLineBreak(eof.leadingTrivia())) {
      return config
          .spacesAfterSemicolon()
          .map(spaces -> "\n" + " ".repeat(spaces))
          .orElse(eof.leadingTrivia());
    }
    return eof.leadingTrivia();
  }

  private boolean containsComment(String trivia) {
    return trivia.indexOf('#') >= 0;
  }

  private boolean hasLineBreak(String trivia) {
    return trivia.indexOf('\n') >= 0;
  }

  private String rewriteWhitespaceBeforeFirstComment(String trivia, String replacement) {
    int comment = trivia.indexOf('#');
    if (comment < 0) {
      return trivia;
    }
    return replacement + trivia.substring(comment);
  }
}
