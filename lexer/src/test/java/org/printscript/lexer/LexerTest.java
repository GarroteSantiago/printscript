package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.printscript.diagnostics.DiagnosticException;
import org.printscript.tokens.Token;
import org.printscript.tokens.TokenType;

class LexerTest {
  private static Lexer lex(String source) {
    return new Lexer(new StringReader(source), KeywordTable.v1());
  }

  @Test
  public void tokenizesColon() {
    assertEquals(TokenType.COLON, lex(":").next().type(), "expected a colon token");
  }

  @Test
  public void tokenizesSemicolon() {
    assertEquals(TokenType.SEMICOLON, lex(";").next().type(), "expected a semicolon token");
  }

  @Test
  public void tokenizesEqual() {
    assertEquals(TokenType.EQUAL, lex("=").next().type(), "expected an equal token");
  }

  @Test
  public void tokenizesPlus() {
    assertEquals(TokenType.PLUS, lex("+").next().type(), "expected a plus token");
  }

  @Test
  public void tokenizesMinus() {
    assertEquals(TokenType.MINUS, lex("-").next().type(), "expected a minus token");
  }

  @Test
  public void tokenizesStar() {
    assertEquals(TokenType.STAR, lex("*").next().type(), "expected a star token");
  }

  @Test
  public void tokenizesSlash() {
    assertEquals(TokenType.SLASH, lex("/").next().type(), "expected a slash token");
  }

  @Test
  public void tokenizesLeftParen() {
    assertEquals(TokenType.LEFT_PAREN, lex("(").next().type(), "expected a left paren token");
  }

  @Test
  public void tokenizesRightParen() {
    assertEquals(TokenType.RIGHT_PAREN, lex(")").next().type(), "expected a right paren token");
  }

  @Test
  public void tokenizesLeftBrace() {
    assertEquals(TokenType.LEFT_BRACE, lex("{").next().type(), "expected a left brace token");
  }

  @Test
  public void tokenizesRightBrace() {
    assertEquals(TokenType.RIGHT_BRACE, lex("}").next().type(), "expected a right brace token");
  }

  @Test
  public void tokenizesADoubleQuotedString() {
    Token token = lex("\"hi\"").next();

    assertEquals(TokenType.STRING, token.type(), "expected a string token");
  }

  @Test
  public void tokenizesADoubleQuotedStringValue() {
    Token token = lex("\"hi\"").next();

    assertEquals("hi", token.semanticLexeme(), "expected the unquoted string value");
  }

  @Test
  public void tokenizesASingleQuotedString() {
    Token token = lex("'hi'").next();

    assertEquals(TokenType.STRING, token.type(), "expected a string token");
  }

  @Test
  public void throwsOnAnUnterminatedString() {
    assertThrows(DiagnosticException.class, () -> lex("\"unterminated").next());
  }

  @Test
  public void tokenizesAnIntegerNumber() {
    Token token = lex("42").next();

    assertEquals(TokenType.NUMBER, token.type(), "expected a number token");
  }

  @Test
  public void tokenizesAnIntegerNumberValue() {
    Token token = lex("42").next();

    assertEquals("42", token.semanticLexeme(), "expected the integer text");
  }

  @Test
  public void tokenizesADecimalNumber() {
    Token token = lex("4.5").next();

    assertEquals("4.5", token.semanticLexeme(), "expected the decimal text");
  }

  @Test
  public void throwsWhenADecimalPointIsNotFollowedByADigit() {
    assertThrows(DiagnosticException.class, () -> lex("4.").next());
  }

  @Test
  public void tokenizesAKeywordUsingTheGivenKeywordTable() {
    Token token = lex("let").next();

    assertEquals(TokenType.LET, token.type(), "expected the let keyword");
  }

  @Test
  public void tokenizesAnIdentifierWithAnUnderscore() {
    Token token = lex("_count").next();

    assertEquals(TokenType.IDENTIFIER, token.type(), "expected an identifier token");
  }

  @Test
  public void tokenizesAnIdentifierWithDigits() {
    Token token = lex("count1").next();

    assertEquals("count1", token.semanticLexeme(), "expected the full identifier text");
  }

  @Test
  public void throwsOnAnUnexpectedCharacter() {
    assertThrows(DiagnosticException.class, () -> lex("@").next());
  }

  @Test
  public void skipsLeadingWhitespaceAsTrivia() {
    Token token = lex("   ;").next();

    assertEquals("   ", token.leadingTrivia(), "expected the whitespace as leading trivia");
  }

  @Test
  public void skipsACommentAsTrivia() {
    Token token = lex("# comment\n;").next();

    assertEquals(TokenType.SEMICOLON, token.type(), "expected the token after the comment");
  }

  @Test
  public void tracksRowAcrossNewlines() {
    Lexer lexer = lex("\n;");
    Token token = lexer.next();

    assertEquals(2, token.span().start().row(), "expected the row to advance past the newline");
  }

  @Test
  public void emitsAnEofTokenAtTheEndOfSource() {
    assertEquals(TokenType.EOF, lex("").next().type(), "expected an eof token");
  }

  @Test
  public void keepsEmittingEofTokensAfterTheFirstOne() {
    Lexer lexer = lex("");
    lexer.next();

    assertEquals(TokenType.EOF, lexer.next().type(), "expected a repeated eof token");
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

    assertThrows(IllegalStateException.class, () -> new Lexer(failingReader, KeywordTable.v1()));
  }

  @Test
  public void preservesEverySourceCharacterAsTriviaOrTokenText() {
    String source =
        """
                # file comment
                let   name:string='Ada'; # inline comment

                println(name);\s\s
                """;
    Lexer lexer = lex(source);
    StringBuilder rebuilt = new StringBuilder();
    Token token;
    do {
      token = lexer.next();
      rebuilt.append(token.leadingTrivia()).append(token.text());
    } while (!TokenType.EOF.equals(token.type()));

    assertEquals(
        source, rebuilt.toString(), "expected every source character preserved as trivia or text");
  }
}
