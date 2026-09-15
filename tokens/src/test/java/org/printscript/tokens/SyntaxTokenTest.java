package org.printscript.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.printscript.source.SourcePosition;
import org.printscript.source.SourceSpan;

class SyntaxTokenTest {
  private static final SourceSpan SPAN = SourceSpan.at(new SourcePosition(1, 1, 0));
  private static final String LET = "let";

  @Test
  void fromCopiesTheTypeFromTheToken() {
    Token token = new Token(TokenType.LET, LET, LET, "", SPAN);

    SyntaxToken syntaxToken = SyntaxToken.from(token);

    assertEquals(TokenType.LET, syntaxToken.type(), "expected the token's type");
  }

  @Test
  void fromCopiesTheSemanticLexemeFromTheToken() {
    Token token = new Token(TokenType.IDENTIFIER, "count", "count", "", SPAN);

    SyntaxToken syntaxToken = SyntaxToken.from(token);

    assertEquals("count", syntaxToken.semanticLexeme(), "expected the token's semantic lexeme");
  }

  @Test
  void fromCopiesTheTextFromTheToken() {
    Token token = new Token(TokenType.STRING, "hi", "\"hi\"", "", SPAN);

    SyntaxToken syntaxToken = SyntaxToken.from(token);

    assertEquals("\"hi\"", syntaxToken.text(), "expected the token's raw text");
  }

  @Test
  void fromCopiesTheLeadingTriviaFromTheToken() {
    Token token = new Token(TokenType.LET, LET, LET, "  ", SPAN);

    SyntaxToken syntaxToken = SyntaxToken.from(token);

    assertEquals("  ", syntaxToken.leadingTrivia(), "expected the token's leading trivia");
  }

  @Test
  void fromCopiesTheSpanFromTheToken() {
    Token token = new Token(TokenType.LET, LET, LET, "", SPAN);

    SyntaxToken syntaxToken = SyntaxToken.from(token);

    assertEquals(SPAN, syntaxToken.span(), "expected the token's span");
  }
}
