package org.printscript.tokens;

public interface TokenTypeVisitor<R> {
  R visitLet();

  R visitIdentifier();

  R visitType();

  R visitNumber();

  R visitString();

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

  R visitConst();

  R visitIf();

  R visitElse();

  R visitBoolean();

  R visitComment();

  R visitEof();
}
