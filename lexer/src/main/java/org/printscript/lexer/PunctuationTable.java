package org.printscript.lexer;

import org.printscript.tokens.TokenType;

@FunctionalInterface
public interface PunctuationTable {
  TokenType classify(Punctuation punctuation);

  static PunctuationTable v1() {
    return punctuation ->
        switch (punctuation) {
          case COLON -> TokenType.COLON;
          case SEMICOLON -> TokenType.SEMICOLON;
          case EQUAL -> TokenType.EQUAL;
          case PLUS -> TokenType.PLUS;
          case MINUS -> TokenType.MINUS;
          case STAR -> TokenType.STAR;
          case SLASH -> TokenType.SLASH;
          case LEFT_PAREN -> TokenType.LEFT_PAREN;
          case RIGHT_PAREN -> TokenType.RIGHT_PAREN;
          case LEFT_BRACE -> TokenType.LEFT_BRACE;
          case RIGHT_BRACE -> TokenType.RIGHT_BRACE;
        };
  }
}
