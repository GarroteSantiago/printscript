package org.printscript.analyzer;

import java.util.regex.Pattern;

@FunctionalInterface
public interface NamingStyleRules {
  boolean matches(NamingStyle style, String name);

  static NamingStyleRules v1() {
    Pattern snakeCase = Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)*");
    Pattern camelCase = Pattern.compile("[a-z][a-zA-Z0-9]*");
    return (style, name) -> style.accept(new V1Visitor(name, snakeCase, camelCase));
  }

  final class V1Visitor implements NamingStyleVisitor<Boolean> {
    private final String name;
    private final Pattern snakeCase;
    private final Pattern camelCase;

    V1Visitor(String name, Pattern snakeCase, Pattern camelCase) {
      this.name = name;
      this.snakeCase = snakeCase;
      this.camelCase = camelCase;
    }

    @Override
    public Boolean visitSnakeCase() {
      return snakeCase.matcher(name).matches();
    }

    @Override
    public Boolean visitCamelCase() {
      return camelCase.matcher(name).matches();
    }
  }
}
