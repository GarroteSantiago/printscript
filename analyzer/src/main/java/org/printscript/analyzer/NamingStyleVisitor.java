package org.printscript.analyzer;

public interface NamingStyleVisitor<R> {
  R visitSnakeCase();

  R visitCamelCase();
}
