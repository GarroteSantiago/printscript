package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.tokens.TokenType;

class KeywordTableBuilderTest {
  @Test
  public void findsEachLexemeOfEachGivenTokenType() {
    KeywordTable table =
        KeywordTableBuilder.build(List.of(TokenType.LET, TokenType.TYPE), Map.of());

    assertEquals(Optional.of(TokenType.LET), table.find("let"), "expected let to be indexed");
  }

  @Test
  public void findsEveryLexemeOfAMultiWordTokenType() {
    KeywordTable table =
        KeywordTableBuilder.build(List.of(TokenType.LET, TokenType.TYPE), Map.of());

    assertEquals(
        Optional.of(TokenType.TYPE), table.find("string"), "expected string to be indexed");
  }

  @Test
  public void doesNotFindWordsOfATypeThatWasNotGiven() {
    KeywordTable table = KeywordTableBuilder.build(List.of(TokenType.LET), Map.of());

    assertTrue(table.find("if").isEmpty(), "expected if to be absent when IF wasn't given");
  }

  @Test
  public void findsTheExtraEntryAlongsideEachTypesOwnLexemes() {
    KeywordTable table =
        KeywordTableBuilder.build(List.of(TokenType.LET), Map.of("boolean", TokenType.TYPE));

    assertEquals(Optional.of(TokenType.TYPE), table.find("boolean"), "expected the extra entry");
  }

  @Test
  public void ignoresTokenTypesWithNoFixedLexeme() {
    KeywordTable table = KeywordTableBuilder.build(List.of(TokenType.IDENTIFIER), Map.of());

    assertTrue(
        table.find("anything").isEmpty(), "expected no entries for a token type with no lexemes");
  }
}
