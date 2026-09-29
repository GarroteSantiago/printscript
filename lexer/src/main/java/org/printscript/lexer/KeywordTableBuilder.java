package org.printscript.lexer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.printscript.tokens.TokenType;

/**
 * Builds a {@link KeywordTable} from each participating type's own {@link TokenType#lexemes()},
 * instead of retyping those strings in a switch. Kept separate from {@link KeywordTable} itself so
 * building (this class) and querying ({@code KeywordTable.of}) can each be tested without the
 * other.
 */
final class KeywordTableBuilder {
  private KeywordTableBuilder() {}

  public static KeywordTable build(List<TokenType> keywordTypes, Map<String, TokenType> extra) {
    Map<String, TokenType> index = new HashMap<>(extra);
    for (TokenType type : keywordTypes) {
      for (String lexeme : type.lexemes()) {
        index.put(lexeme, type);
      }
    }
    return KeywordTable.of(index);
  }
}
