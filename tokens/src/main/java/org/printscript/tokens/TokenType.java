package org.printscript.tokens;

import java.util.List;

public interface TokenType {
  <R> R accept(TokenTypeVisitor<R> visitor);

  /**
   * The reserved words that produce this token, if any (e.g. {@code "let"} for {@link #LET}). Empty
   * for token kinds that aren't spelled as a fixed keyword (operators, literals, {@code
   * IDENTIFIER}, ...). This is the single source of truth {@code KeywordTable} builds from, instead
   * of retyping these strings in its own switch.
   */
  default List<String> lexemes() {
    return List.of();
  }

  TokenType LET =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitLet();
        }

        @Override
        public List<String> lexemes() {
          return List.of("let");
        }

        @Override
        public String toString() {
          return "LET";
        }
      };

  TokenType IDENTIFIER =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitIdentifier();
        }

        @Override
        public String toString() {
          return "IDENTIFIER";
        }
      };

  TokenType TYPE =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitType();
        }

        @Override
        public List<String> lexemes() {
          return List.of("number", "string");
        }

        @Override
        public String toString() {
          return "TYPE";
        }
      };

  TokenType NUMBER =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitNumber();
        }

        @Override
        public String toString() {
          return "NUMBER";
        }
      };

  TokenType STRING =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitString();
        }

        @Override
        public String toString() {
          return "STRING";
        }
      };

  TokenType COLON =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitColon();
        }

        @Override
        public String toString() {
          return "COLON";
        }
      };

  TokenType SEMICOLON =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitSemicolon();
        }

        @Override
        public String toString() {
          return "SEMICOLON";
        }
      };

  TokenType EQUAL =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitEqual();
        }

        @Override
        public String toString() {
          return "EQUAL";
        }
      };

  TokenType PLUS =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitPlus();
        }

        @Override
        public String toString() {
          return "PLUS";
        }
      };

  TokenType MINUS =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitMinus();
        }

        @Override
        public String toString() {
          return "MINUS";
        }
      };

  TokenType STAR =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitStar();
        }

        @Override
        public String toString() {
          return "STAR";
        }
      };

  TokenType SLASH =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitSlash();
        }

        @Override
        public String toString() {
          return "SLASH";
        }
      };

  TokenType LEFT_PAREN =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitLeftParen();
        }

        @Override
        public String toString() {
          return "LEFT_PAREN";
        }
      };

  TokenType RIGHT_PAREN =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitRightParen();
        }

        @Override
        public String toString() {
          return "RIGHT_PAREN";
        }
      };

  TokenType LEFT_BRACE =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitLeftBrace();
        }

        @Override
        public String toString() {
          return "LEFT_BRACE";
        }
      };

  TokenType RIGHT_BRACE =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitRightBrace();
        }

        @Override
        public String toString() {
          return "RIGHT_BRACE";
        }
      };

  TokenType CONST =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitConst();
        }

        @Override
        public List<String> lexemes() {
          return List.of("const");
        }

        @Override
        public String toString() {
          return "CONST";
        }
      };

  TokenType IF =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitIf();
        }

        @Override
        public List<String> lexemes() {
          return List.of("if");
        }

        @Override
        public String toString() {
          return "IF";
        }
      };

  TokenType ELSE =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitElse();
        }

        @Override
        public List<String> lexemes() {
          return List.of("else");
        }

        @Override
        public String toString() {
          return "ELSE";
        }
      };

  TokenType BOOLEAN =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitBoolean();
        }

        @Override
        public List<String> lexemes() {
          return List.of("true", "false");
        }

        @Override
        public String toString() {
          return "BOOLEAN";
        }
      };

  TokenType COMMENT =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitComment();
        }

        @Override
        public String toString() {
          return "COMMENT";
        }
      };

  TokenType EOF =
      new TokenType() {
        @Override
        public <R> R accept(TokenTypeVisitor<R> visitor) {
          return visitor.visitEof();
        }

        @Override
        public String toString() {
          return "EOF";
        }
      };
}
