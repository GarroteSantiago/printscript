package org.printscript.interpreter;

public interface RuntimeValueVisitor<R> {
  R visitNumber(RuntimeValue.NumberValue value);

  R visitString(RuntimeValue.StringValue value);

  R visitBoolean(RuntimeValue.BooleanValue value);

  R visitUnit(RuntimeValue.UnitValue value);
}
