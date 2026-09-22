package org.printscript.syntax;

public interface TypeNameVisitor<R> {
  R visitNumber();

  R visitString();

  R visitBoolean();
}
