package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.printscript.tokens.TokenType;

class KeywordTableTest {
  @Test
  void v1ClassifiesLetAsTheLetKeyword() {
    assertEquals(TokenType.LET, KeywordTable.v1().classify("let"), "expected let to be a keyword");
  }

  @Test
  void v1ClassifiesNumberAsATypeAnnotation() {
    assertEquals(TokenType.TYPE, KeywordTable.v1().classify("number"), "expected number as type");
  }

  @Test
  void v1ClassifiesStringAsATypeAnnotation() {
    assertEquals(TokenType.TYPE, KeywordTable.v1().classify("string"), "expected string as type");
  }

  @Test
  void v1ClassifiesUnknownWordsAsIdentifiers() {
    assertEquals(
        TokenType.IDENTIFIER, KeywordTable.v1().classify("total"), "expected an identifier");
  }

  @Test
  void v1DoesNotRecognizeConstAsAKeyword() {
    assertEquals(
        TokenType.IDENTIFIER,
        KeywordTable.v1().classify("const"),
        "expected v1 to treat const as a plain identifier");
  }

  @Test
  void v11ClassifiesConstAsAKeyword() {
    assertEquals(
        TokenType.CONST, KeywordTable.v1_1().classify("const"), "expected const as a keyword");
  }

  @Test
  void v11ClassifiesIfAsAKeyword() {
    assertEquals(TokenType.IF, KeywordTable.v1_1().classify("if"), "expected if as a keyword");
  }

  @Test
  void v11ClassifiesElseAsAKeyword() {
    assertEquals(
        TokenType.ELSE, KeywordTable.v1_1().classify("else"), "expected else as a keyword");
  }

  @Test
  void v11ClassifiesBooleanAsATypeAnnotation() {
    assertEquals(
        TokenType.TYPE, KeywordTable.v1_1().classify("boolean"), "expected boolean as a type");
  }

  @Test
  void v11ClassifiesTrueAsABooleanLiteral() {
    assertEquals(
        TokenType.BOOLEAN, KeywordTable.v1_1().classify("true"), "expected true as a boolean");
  }

  @Test
  void v11ClassifiesFalseAsABooleanLiteral() {
    assertEquals(
        TokenType.BOOLEAN, KeywordTable.v1_1().classify("false"), "expected false as a boolean");
  }

  @Test
  void v11ClassifiesUnknownWordsAsIdentifiers() {
    assertEquals(
        TokenType.IDENTIFIER, KeywordTable.v1_1().classify("total"), "expected an identifier");
  }
}
