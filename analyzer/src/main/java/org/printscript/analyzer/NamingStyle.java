package org.printscript.analyzer;

public interface NamingStyle {
  <R> R accept(NamingStyleVisitor<R> visitor);

  NamingStyle SNAKE_CASE =
      new NamingStyle() {
        @Override
        public <R> R accept(NamingStyleVisitor<R> visitor) {
          return visitor.visitSnakeCase();
        }

        @Override
        public String toString() {
          return "SNAKE_CASE";
        }
      };

  NamingStyle CAMEL_CASE =
      new NamingStyle() {
        @Override
        public <R> R accept(NamingStyleVisitor<R> visitor) {
          return visitor.visitCamelCase();
        }

        @Override
        public String toString() {
          return "CAMEL_CASE";
        }
      };
}
