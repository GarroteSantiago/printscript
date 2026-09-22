package org.printscript.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TokenTypeTest {
  private static Stream<Arguments> tokenTypes() {
    return Stream.of(
        Arguments.of(TokenType.LET, "LET", List.of("let")),
        Arguments.of(TokenType.IDENTIFIER, "IDENTIFIER", List.of()),
        Arguments.of(TokenType.TYPE, "TYPE", List.of("number", "string")),
        Arguments.of(TokenType.NUMBER, "NUMBER", List.of()),
        Arguments.of(TokenType.STRING, "STRING", List.of()),
        Arguments.of(TokenType.COLON, "COLON", List.of()),
        Arguments.of(TokenType.SEMICOLON, "SEMICOLON", List.of()),
        Arguments.of(TokenType.EQUAL, "EQUAL", List.of()),
        Arguments.of(TokenType.PLUS, "PLUS", List.of()),
        Arguments.of(TokenType.MINUS, "MINUS", List.of()),
        Arguments.of(TokenType.STAR, "STAR", List.of()),
        Arguments.of(TokenType.SLASH, "SLASH", List.of()),
        Arguments.of(TokenType.LEFT_PAREN, "LEFT_PAREN", List.of()),
        Arguments.of(TokenType.RIGHT_PAREN, "RIGHT_PAREN", List.of()),
        Arguments.of(TokenType.LEFT_BRACE, "LEFT_BRACE", List.of()),
        Arguments.of(TokenType.RIGHT_BRACE, "RIGHT_BRACE", List.of()),
        Arguments.of(TokenType.CONST, "CONST", List.of("const")),
        Arguments.of(TokenType.IF, "IF", List.of("if")),
        Arguments.of(TokenType.ELSE, "ELSE", List.of("else")),
        Arguments.of(TokenType.BOOLEAN, "BOOLEAN", List.of("true", "false")),
        Arguments.of(TokenType.COMMENT, "COMMENT", List.of()),
        Arguments.of(TokenType.EOF, "EOF", List.of()));
  }

  @ParameterizedTest
  @MethodSource("tokenTypes")
  void toStringNamesTheConstant(TokenType type, String name, List<String> lexemes) {
    assertEquals(name, type.toString(), "expected toString() to name the constant");
  }

  @ParameterizedTest
  @MethodSource("tokenTypes")
  void lexemesMatchTheReservedWords(TokenType type, String name, List<String> lexemes) {
    assertEquals(lexemes, type.lexemes(), "expected the reserved words for " + name);
  }

  @ParameterizedTest
  @MethodSource("tokenTypes")
  void acceptDispatchesToTheMatchingVisitorMethod(
      TokenType type, String name, List<String> lexemes) {
    assertEquals(
        name,
        type.accept(NAMING_VISITOR),
        "expected accept() to dispatch to the visitor method for " + name);
  }

  private static final TokenTypeVisitor<String> NAMING_VISITOR =
      new TokenTypeVisitor<>() {
        @Override
        public String visitLet() {
          return "LET";
        }

        @Override
        public String visitIdentifier() {
          return "IDENTIFIER";
        }

        @Override
        public String visitType() {
          return "TYPE";
        }

        @Override
        public String visitNumber() {
          return "NUMBER";
        }

        @Override
        public String visitString() {
          return "STRING";
        }

        @Override
        public String visitColon() {
          return "COLON";
        }

        @Override
        public String visitSemicolon() {
          return "SEMICOLON";
        }

        @Override
        public String visitEqual() {
          return "EQUAL";
        }

        @Override
        public String visitPlus() {
          return "PLUS";
        }

        @Override
        public String visitMinus() {
          return "MINUS";
        }

        @Override
        public String visitStar() {
          return "STAR";
        }

        @Override
        public String visitSlash() {
          return "SLASH";
        }

        @Override
        public String visitLeftParen() {
          return "LEFT_PAREN";
        }

        @Override
        public String visitRightParen() {
          return "RIGHT_PAREN";
        }

        @Override
        public String visitLeftBrace() {
          return "LEFT_BRACE";
        }

        @Override
        public String visitRightBrace() {
          return "RIGHT_BRACE";
        }

        @Override
        public String visitConst() {
          return "CONST";
        }

        @Override
        public String visitIf() {
          return "IF";
        }

        @Override
        public String visitElse() {
          return "ELSE";
        }

        @Override
        public String visitBoolean() {
          return "BOOLEAN";
        }

        @Override
        public String visitComment() {
          return "COMMENT";
        }

        @Override
        public String visitEof() {
          return "EOF";
        }
      };
}
