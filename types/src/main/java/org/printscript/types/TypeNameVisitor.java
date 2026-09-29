package org.printscript.types;

public interface TypeNameVisitor<R> {
  R visitNumber();

  R visitString();

  R visitBoolean();
}
