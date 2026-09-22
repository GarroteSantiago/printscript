package org.printscript.lexer;

public interface PunctuationVisitor<R> {
  R visitColon();

  R visitSemicolon();

  R visitEqual();

  R visitPlus();

  R visitMinus();

  R visitStar();

  R visitSlash();

  R visitLeftParen();

  R visitRightParen();

  R visitLeftBrace();

  R visitRightBrace();
}
