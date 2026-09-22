package org.printscript.lexer;

import org.printscript.tokens.TokenType;

/**
 * Classifies a scanned {@link Punctuation} symbol into its {@link TokenType}, via {@link
 * PunctuationVisitor} double-dispatch rather than a switch over {@link Punctuation#symbol()}.
 * Unlike {@link KeywordTable}, this has had only one version so far — {@link #v1()} — since no
 * language version has changed what a punctuation character means.
 */
@FunctionalInterface
public interface PunctuationTable {
  TokenType classify(Punctuation punctuation);

  static PunctuationTable v1() {
    return punctuation -> punctuation.accept(new V1Visitor());
  }

  final class V1Visitor implements PunctuationVisitor<TokenType> {
    @Override
    public TokenType visitColon() {
      return TokenType.COLON;
    }

    @Override
    public TokenType visitSemicolon() {
      return TokenType.SEMICOLON;
    }

    @Override
    public TokenType visitEqual() {
      return TokenType.EQUAL;
    }

    @Override
    public TokenType visitPlus() {
      return TokenType.PLUS;
    }

    @Override
    public TokenType visitMinus() {
      return TokenType.MINUS;
    }

    @Override
    public TokenType visitStar() {
      return TokenType.STAR;
    }

    @Override
    public TokenType visitSlash() {
      return TokenType.SLASH;
    }

    @Override
    public TokenType visitLeftParen() {
      return TokenType.LEFT_PAREN;
    }

    @Override
    public TokenType visitRightParen() {
      return TokenType.RIGHT_PAREN;
    }

    @Override
    public TokenType visitLeftBrace() {
      return TokenType.LEFT_BRACE;
    }

    @Override
    public TokenType visitRightBrace() {
      return TokenType.RIGHT_BRACE;
    }
  }
}
