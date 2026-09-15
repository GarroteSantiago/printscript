package org.printscript.lexer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.diagnostics.Phase;
import org.printscript.source.SourcePosition;
import org.printscript.source.SourceSpan;
import org.printscript.tokens.SyntaxException;
import org.printscript.tokens.Token;
import org.printscript.tokens.TokenSource;
import org.printscript.tokens.TokenType;

public final class Lexer implements TokenSource {
  private static final char NEWLINE = '\n';
  private static final char COMMENT_MARKER = '#';

  private final Reader reader;
  private final KeywordTable keywords;
  private final PunctuationTable punctuation = PunctuationTable.v1();
  private int currentChar;
  private boolean eofTokenEmitted;
  private int row = 1;
  private int column = 1;
  private int offset;
  private SourcePosition lastConsumed = new SourcePosition(1, 1, 0);

  public Lexer(String source) {
    this(new StringReader(source), KeywordTable.v1());
  }

  public Lexer(Reader reader) {
    this(reader, KeywordTable.v1());
  }

  public Lexer(String source, KeywordTable keywords) {
    this(new StringReader(source), keywords);
  }

  public Lexer(Reader reader, KeywordTable keywords) {
    this.reader = reader;
    this.keywords = keywords;
    this.currentChar = readRaw();
  }

  @Override
  public Token next() {
    String leadingTrivia = consumeTrivia();
    if (isAtEnd()) {
      if (eofTokenEmitted) return eof(leadingTrivia);
      eofTokenEmitted = true;
      return eof(leadingTrivia);
    }
    SourcePosition start = position();
    char c = advance();
    if (c == '\'' || c == '"') {
      return string(c, leadingTrivia, start);
    }
    Optional<Punctuation> symbol = Punctuation.lookup(c);
    if (symbol.isPresent()) {
      String text = String.valueOf(c);
      return token(punctuation.classify(symbol.get()), text, text, leadingTrivia, start);
    }
    if (Character.isDigit(c)) {
      return number(c, leadingTrivia, start);
    }
    if (isIdentifierStart(c)) {
      return identifier(c, leadingTrivia, start);
    }
    throw syntaxError("Unexpected character '" + c + "'", start);
  }

  private String consumeTrivia() {
    StringBuilder trivia = new StringBuilder();
    while (!isAtEnd()) {
      char c = peek();
      if (c == ' ' || c == '\r' || c == '\t') {
        trivia.append(advance());
      } else if (c == NEWLINE) {
        trivia.append(advance());
      } else if (c == COMMENT_MARKER) {
        while (!isAtEnd() && peek() != NEWLINE) {
          trivia.append(advance());
        }
      } else {
        break;
      }
    }
    return trivia.toString();
  }

  private Token string(char quote, String leadingTrivia, SourcePosition start) {
    StringBuilder text = new StringBuilder().append(quote);
    StringBuilder value = new StringBuilder();
    while (!isAtEnd() && peek() != quote) {
      char c = advance();
      text.append(c);
      value.append(c);
    }
    if (isAtEnd()) {
      throw syntaxError("Unterminated string literal", start);
    }
    text.append(advance());
    return token(TokenType.STRING, value.toString(), text.toString(), leadingTrivia, start);
  }

  private Token number(char first, String leadingTrivia, SourcePosition start) {
    StringBuilder text = new StringBuilder().append(first);
    while (!isAtEnd() && Character.isDigit(peek())) {
      text.append(advance());
    }
    if (!isAtEnd() && peek() == '.') {
      text.append(advance());
      if (isAtEnd() || !Character.isDigit(peek())) {
        throw syntaxError("Expected digit after decimal point", lastConsumed);
      }
      while (!isAtEnd() && Character.isDigit(peek())) {
        text.append(advance());
      }
    }
    return token(TokenType.NUMBER, text.toString(), text.toString(), leadingTrivia, start);
  }

  private Token identifier(char first, String leadingTrivia, SourcePosition start) {
    StringBuilder text = new StringBuilder().append(first);
    while (!isAtEnd() && isIdentifierPart(peek())) {
      text.append(advance());
    }
    String lexeme = text.toString();
    TokenType type = keywords.classify(lexeme);
    return token(type, lexeme, lexeme, leadingTrivia, start);
  }

  private Token eof(String leadingTrivia) {
    return new Token(TokenType.EOF, "", "", leadingTrivia, SourceSpan.at(position()));
  }

  private Token token(
      TokenType type,
      String semanticLexeme,
      String text,
      String leadingTrivia,
      SourcePosition start) {
    return new Token(
        type, semanticLexeme, text, leadingTrivia, new SourceSpan(start, lastConsumed));
  }

  private char peek() {
    return (char) currentChar;
  }

  private boolean isAtEnd() {
    return currentChar == -1;
  }

  private char advance() {
    SourcePosition before = position();
    char c = (char) currentChar;
    currentChar = readRaw();
    lastConsumed = before;
    offset++;
    if (c == NEWLINE) {
      row++;
      column = 1;
    } else {
      column++;
    }
    return c;
  }

  private int readRaw() {
    try {
      return reader.read();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not read source", exception);
    }
  }

  private SourcePosition position() {
    return new SourcePosition(row, column, offset);
  }

  private SyntaxException syntaxError(String message, SourcePosition position) {
    return new SyntaxException(Diagnostic.error(Phase.SYNTAX, message, SourceSpan.at(position)));
  }

  private static boolean isIdentifierStart(char c) {
    return Character.isLetter(c) || c == '_';
  }

  private static boolean isIdentifierPart(char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }
}
