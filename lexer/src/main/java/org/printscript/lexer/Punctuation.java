package org.printscript.lexer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface Punctuation {
  char symbol();

  <R> R accept(PunctuationVisitor<R> visitor);

  Punctuation COLON =
      new Punctuation() {
        @Override
        public char symbol() {
          return ':';
        }

        @Override
        public <R> R accept(PunctuationVisitor<R> visitor) {
          return visitor.visitColon();
        }

        @Override
        public String toString() {
          return "COLON";
        }
      };

  Punctuation SEMICOLON =
      new Punctuation() {
        @Override
        public char symbol() {
          return ';';
        }

        @Override
        public <R> R accept(PunctuationVisitor<R> visitor) {
          return visitor.visitSemicolon();
        }

        @Override
        public String toString() {
          return "SEMICOLON";
        }
      };

  Punctuation EQUAL =
      new Punctuation() {
        @Override
        public char symbol() {
          return '=';
        }

        @Override
        public <R> R accept(PunctuationVisitor<R> visitor) {
          return visitor.visitEqual();
        }

        @Override
        public String toString() {
          return "EQUAL";
        }
      };

  Punctuation PLUS =
      new Punctuation() {
        @Override
        public char symbol() {
          return '+';
        }

        @Override
        public <R> R accept(PunctuationVisitor<R> visitor) {
          return visitor.visitPlus();
        }

        @Override
        public String toString() {
          return "PLUS";
        }
      };

  Punctuation MINUS =
      new Punctuation() {
        @Override
        public char symbol() {
          return '-';
        }

        @Override
        public <R> R accept(PunctuationVisitor<R> visitor) {
          return visitor.visitMinus();
        }

        @Override
        public String toString() {
          return "MINUS";
        }
      };

  Punctuation STAR =
      new Punctuation() {
        @Override
        public char symbol() {
          return '*';
        }

        @Override
        public <R> R accept(PunctuationVisitor<R> visitor) {
          return visitor.visitStar();
        }

        @Override
        public String toString() {
          return "STAR";
        }
      };

  Punctuation SLASH =
      new Punctuation() {
        @Override
        public char symbol() {
          return '/';
        }

        @Override
        public <R> R accept(PunctuationVisitor<R> visitor) {
          return visitor.visitSlash();
        }

        @Override
        public String toString() {
          return "SLASH";
        }
      };

  Punctuation LEFT_PAREN =
      new Punctuation() {
        @Override
        public char symbol() {
          return '(';
        }

        @Override
        public <R> R accept(PunctuationVisitor<R> visitor) {
          return visitor.visitLeftParen();
        }

        @Override
        public String toString() {
          return "LEFT_PAREN";
        }
      };

  Punctuation RIGHT_PAREN =
      new Punctuation() {
        @Override
        public char symbol() {
          return ')';
        }

        @Override
        public <R> R accept(PunctuationVisitor<R> visitor) {
          return visitor.visitRightParen();
        }

        @Override
        public String toString() {
          return "RIGHT_PAREN";
        }
      };

  Punctuation LEFT_BRACE =
      new Punctuation() {
        @Override
        public char symbol() {
          return '{';
        }

        @Override
        public <R> R accept(PunctuationVisitor<R> visitor) {
          return visitor.visitLeftBrace();
        }

        @Override
        public String toString() {
          return "LEFT_BRACE";
        }
      };

  Punctuation RIGHT_BRACE =
      new Punctuation() {
        @Override
        public char symbol() {
          return '}';
        }

        @Override
        public <R> R accept(PunctuationVisitor<R> visitor) {
          return visitor.visitRightBrace();
        }

        @Override
        public String toString() {
          return "RIGHT_BRACE";
        }
      };

  List<Punctuation> ALL =
      List.of(
          COLON,
          SEMICOLON,
          EQUAL,
          PLUS,
          MINUS,
          STAR,
          SLASH,
          LEFT_PAREN,
          RIGHT_PAREN,
          LEFT_BRACE,
          RIGHT_BRACE);

  Map<Character, Punctuation> BY_SYMBOL =
      ALL.stream().collect(Collectors.toMap(Punctuation::symbol, punctuation -> punctuation));

  static Optional<Punctuation> lookup(char c) {
    return Optional.ofNullable(BY_SYMBOL.get(c));
  }
}
