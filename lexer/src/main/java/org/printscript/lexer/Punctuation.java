package org.printscript.lexer;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Punctuation {
  COLON(':'),
  SEMICOLON(';'),
  EQUAL('='),
  PLUS('+'),
  MINUS('-'),
  STAR('*'),
  SLASH('/'),
  LEFT_PAREN('('),
  RIGHT_PAREN(')'),
  LEFT_BRACE('{'),
  RIGHT_BRACE('}');

  private static final Map<Character, Punctuation> BY_SYMBOL =
      Stream.of(values())
          .collect(Collectors.toMap(Punctuation::symbol, punctuation -> punctuation));

  private final char symbol;

  Punctuation(char symbol) {
    this.symbol = symbol;
  }

  public char symbol() {
    return symbol;
  }

  public static Optional<Punctuation> lookup(char c) {
    return Optional.ofNullable(BY_SYMBOL.get(c));
  }
}
