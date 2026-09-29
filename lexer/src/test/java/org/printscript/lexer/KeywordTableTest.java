package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.tokens.TokenType;

class KeywordTableTest {
  private static final String LET = "let";

  @Test
  public void ofFindsAGivenEntryWithoutAnyVersionWiring() {
    KeywordTable table = KeywordTable.of(Map.of(LET, TokenType.LET));

    assertEquals(Optional.of(TokenType.LET), table.find(LET), "expected the given entry");
  }

  @Test
  public void ofDoesNotFindAnUnindexedWord() {
    KeywordTable table = KeywordTable.of(Map.of(LET, TokenType.LET));

    assertTrue(table.find("total").isEmpty(), "expected no match for an unindexed word");
  }

  @Test
  public void v1FindsLetAsTheLetKeyword() {
    assertEquals(
        Optional.of(TokenType.LET), KeywordTable.v1().find(LET), "expected let to be a keyword");
  }

  @Test
  public void v1FindsNumberAsATypeAnnotation() {
    assertEquals(
        Optional.of(TokenType.TYPE), KeywordTable.v1().find("number"), "expected number as type");
  }

  @Test
  public void v1FindsStringAsATypeAnnotation() {
    assertEquals(
        Optional.of(TokenType.TYPE), KeywordTable.v1().find("string"), "expected string as type");
  }

  @Test
  public void v1DoesNotFindUnknownWords() {
    assertTrue(KeywordTable.v1().find("total").isEmpty(), "expected no reserved word match");
  }

  @Test
  public void v1DoesNotRecognizeConstAsAKeyword() {
    assertTrue(
        KeywordTable.v1().find("const").isEmpty(),
        "expected v1 to not treat const as a reserved word");
  }

  @Test
  public void v11FindsConstAsAKeyword() {
    assertEquals(
        Optional.of(TokenType.CONST),
        KeywordTable.v1_1().find("const"),
        "expected const as a keyword");
  }

  @Test
  public void v11FindsIfAsAKeyword() {
    assertEquals(
        Optional.of(TokenType.IF), KeywordTable.v1_1().find("if"), "expected if as a keyword");
  }

  @Test
  public void v11FindsElseAsAKeyword() {
    assertEquals(
        Optional.of(TokenType.ELSE),
        KeywordTable.v1_1().find("else"),
        "expected else as a keyword");
  }

  @Test
  public void v11FindsBooleanAsATypeAnnotation() {
    assertEquals(
        Optional.of(TokenType.TYPE),
        KeywordTable.v1_1().find("boolean"),
        "expected boolean as a type");
  }

  @Test
  public void v11FindsTrueAsABooleanLiteral() {
    assertEquals(
        Optional.of(TokenType.BOOLEAN),
        KeywordTable.v1_1().find("true"),
        "expected true as a boolean");
  }

  @Test
  public void v11FindsFalseAsABooleanLiteral() {
    assertEquals(
        Optional.of(TokenType.BOOLEAN),
        KeywordTable.v1_1().find("false"),
        "expected false as a boolean");
  }

  @Test
  public void v11DoesNotFindUnknownWords() {
    assertTrue(KeywordTable.v1_1().find("total").isEmpty(), "expected no reserved word match");
  }
}
