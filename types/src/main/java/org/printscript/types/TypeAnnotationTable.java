package org.printscript.types;

@FunctionalInterface
public interface TypeAnnotationTable {
  TypeName resolve(String lexeme);

  static TypeAnnotationTable v1() {
    return lexeme ->
        switch (lexeme) {
          case "number" -> TypeName.NUMBER;
          case "string" -> TypeName.STRING;
          default -> throw new IllegalArgumentException("Unknown type: " + lexeme);
        };
  }

  static TypeAnnotationTable v1_1() {
    return lexeme ->
        switch (lexeme) {
          case "number" -> TypeName.NUMBER;
          case "string" -> TypeName.STRING;
          case "boolean" -> TypeName.BOOLEAN;
          default -> throw new IllegalArgumentException("Unknown type: " + lexeme);
        };
  }
}
