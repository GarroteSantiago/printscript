package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.printscript.tokens.SyntaxException;
import org.printscript.tokens.Token;
import org.printscript.tokens.TokenType;

class LexerTest {
  @Test
  public void tokenizesColon() {
    assertEquals(TokenType.COLON, new Lexer(":").next().type(), "expected a colon token");
  }

  @Test
  public void tokenizesSemicolon() {
    assertEquals(TokenType.SEMICOLON, new Lexer(";").next().type(), "expected a semicolon token");
  }

  @Test
  public void tokenizesEqual() {
    assertEquals(TokenType.EQUAL, new Lexer("=").next().type(), "expected an equal token");
  }

  @Test
  public void tokenizesPlus() {
    assertEquals(TokenType.PLUS, new Lexer("+").next().type(), "expected a plus token");
  }

  @Test
  public void tokenizesMinus() {
    assertEquals(TokenType.MINUS, new Lexer("-").next().type(), "expected a minus token");
  }

  @Test
  public void tokenizesStar() {
    assertEquals(TokenType.STAR, new Lexer("*").next().type(), "expected a star token");
  }

  @Test
  public void tokenizesSlash() {
    assertEquals(TokenType.SLASH, new Lexer("/").next().type(), "expected a slash token");
  }

  @Test
  public void tokenizesLeftParen() {
    assertEquals(TokenType.LEFT_PAREN, new Lexer("(").next().type(), "expected a left paren token");
  }

  @Test
  public void tokenizesRightParen() {
    assertEquals(
        TokenType.RIGHT_PAREN, new Lexer(")").next().type(), "expected a right paren token");
  }

  @Test
  public void tokenizesLeftBrace() {
    assertEquals(TokenType.LEFT_BRACE, new Lexer("{").next().type(), "expected a left brace token");
  }

  @Test
  public void tokenizesRightBrace() {
    assertEquals(
        TokenType.RIGHT_BRACE, new Lexer("}").next().type(), "expected a right brace token");
  }

  @Test
  public void tokenizesADoubleQuotedString() {
    Token token = new Lexer("\"hi\"").next();

    assertEquals(TokenType.STRING, token.type(), "expected a string token");
  }

  @Test
  public void tokenizesADoubleQuotedStringValue() {
    Token token = new Lexer("\"hi\"").next();

    assertEquals("hi", token.semanticLexeme(), "expected the unquoted string value");
  }

  @Test
  public void tokenizesASingleQuotedString() {
    Token token = new Lexer("'hi'").next();

    assertEquals(TokenType.STRING, token.type(), "expected a string token");
  }

  @Test
  public void throwsOnAnUnterminatedString() {
    assertThrows(SyntaxException.class, () -> new Lexer("\"unterminated").next());
  }

  @Test
  public void tokenizesAnIntegerNumber() {
    Token token = new Lexer("42").next();

    assertEquals(TokenType.NUMBER, token.type(), "expected a number token");
  }

  @Test
  public void tokenizesAnIntegerNumberValue() {
    Token token = new Lexer("42").next();

    assertEquals("42", token.semanticLexeme(), "expected the integer text");
  }

  @Test
  public void tokenizesADecimalNumber() {
    Token token = new Lexer("4.5").next();

    assertEquals("4.5", token.semanticLexeme(), "expected the decimal text");
  }

  @Test
  public void throwsWhenADecimalPointIsNotFollowedByADigit() {
    assertThrows(SyntaxException.class, () -> new Lexer("4.").next());
  }

  @Test
  public void tokenizesAKeywordUsingTheGivenKeywordTable() {
    Token token = new Lexer("let", KeywordTable.v1()).next();

    assertEquals(TokenType.LET, token.type(), "expected the let keyword");
  }

  @Test
  public void tokenizesAnIdentifierWithAnUnderscore() {
    Token token = new Lexer("_count").next();

    assertEquals(TokenType.IDENTIFIER, token.type(), "expected an identifier token");
  }

  @Test
  public void tokenizesAnIdentifierWithDigits() {
    Token token = new Lexer("count1").next();

    assertEquals("count1", token.semanticLexeme(), "expected the full identifier text");
  }

  @Test
  public void throwsOnAnUnexpectedCharacter() {
    assertThrows(SyntaxException.class, () -> new Lexer("@").next());
  }

  @Test
  public void skipsLeadingWhitespaceAsTrivia() {
    Token token = new Lexer("   ;").next();

    assertEquals("   ", token.leadingTrivia(), "expected the whitespace as leading trivia");
  }

  @Test
  public void skipsACommentAsTrivia() {
    Token token = new Lexer("# comment\n;").next();

    assertEquals(TokenType.SEMICOLON, token.type(), "expected the token after the comment");
  }

  @Test
  public void tracksRowAcrossNewlines() {
    Lexer lexer = new Lexer("\n;");
    Token token = lexer.next();

    assertEquals(2, token.span().start().row(), "expected the row to advance past the newline");
  }

  @Test
  public void emitsAnEofTokenAtTheEndOfSource() {
    assertEquals(TokenType.EOF, new Lexer("").next().type(), "expected an eof token");
  }

  @Test
  public void keepsEmittingEofTokensAfterTheFirstOne() {
    Lexer lexer = new Lexer("");
    lexer.next();

    assertEquals(TokenType.EOF, lexer.next().type(), "expected a repeated eof token");
  }

  @Test
  @SuppressWarnings(
      "PMD.CloseResource") // the reader backs the Lexer for the duration of the test; the JVM
  // reclaims it at test end
  void constructsFromAPlainReader() {
    Reader reader = new StringReader(";");

    assertEquals(
        TokenType.SEMICOLON, new Lexer(reader).next().type(), "expected a semicolon token");
  }

  @Test
  @SuppressWarnings(
      "PMD.CloseResource") // the reader backs the Lexer for the duration of the test; the JVM
  // reclaims it at test end
  void constructsFromAReaderWithAKeywordTable() {
    Reader reader = new StringReader("let");

    assertEquals(
        TokenType.LET,
        new Lexer(reader, KeywordTable.v1()).next().type(),
        "expected the let keyword");
  }

  @Test
  @SuppressWarnings(
      "PMD.CloseResource") // the reader backs the Lexer for the duration of the test; the JVM
  // reclaims it at test end
  void wrapsAnIoExceptionFromTheReaderAsAnIllegalStateException() {
    Reader failingReader =
        new Reader() {
          @Override
          public int read(char[] buffer, int offset, int length) throws IOException {
            throw new IOException("boom");
          }

          @Override
          public void close() {}
        };

    assertThrows(IllegalStateException.class, () -> new Lexer(failingReader));
  }
}
