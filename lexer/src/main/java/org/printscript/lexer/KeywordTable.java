package org.printscript.lexer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.printscript.tokens.TokenType;

/**
 * Answers "is this scanned word one of our reserved words?" Absent means "not reserved" — it does
 * NOT mean {@code IDENTIFIER}; that fallback belongs to whoever knows the word was scanned as an
 * identifier in the first place (see {@code Lexer.identifier()}).
 */
@FunctionalInterface
public interface KeywordTable {
  Optional<TokenType> find(String lexeme);

  static KeywordTable of(Map<String, TokenType> index) {
    return lexeme -> Optional.ofNullable(index.get(lexeme));
  }

  static KeywordTable v1() {
    return KeywordTableBuilder.build(List.of(TokenType.LET, TokenType.TYPE), Map.of());
  }

  static KeywordTable v1_1() {
    return KeywordTableBuilder.build(
        List.of(
            TokenType.LET,
            TokenType.CONST,
            TokenType.IF,
            TokenType.ELSE,
            TokenType.TYPE,
            TokenType.BOOLEAN),
        // "boolean" only became a recognized type annotation in v1.1; TokenType.TYPE.lexemes()
        // stays fixed at ["number", "string"] since that part is unchanged since v1.
        Map.of("boolean", TokenType.TYPE));
  }
}
