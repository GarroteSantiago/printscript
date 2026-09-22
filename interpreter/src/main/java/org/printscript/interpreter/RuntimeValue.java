package org.printscript.interpreter;

import java.math.BigDecimal;
import org.printscript.syntax.TypeName;

public interface RuntimeValue {
  TypeName type();

  <R> R accept(RuntimeValueVisitor<R> visitor);

  record NumberValue(BigDecimal value) implements RuntimeValue {
    @Override
    public TypeName type() {
      return TypeName.NUMBER;
    }

    @Override
    public <R> R accept(RuntimeValueVisitor<R> visitor) {
      return visitor.visitNumber(this);
    }
  }

  record StringValue(String value) implements RuntimeValue {
    @Override
    public TypeName type() {
      return TypeName.STRING;
    }

    @Override
    public <R> R accept(RuntimeValueVisitor<R> visitor) {
      return visitor.visitString(this);
    }
  }

  record BooleanValue(boolean value) implements RuntimeValue {
    @Override
    public TypeName type() {
      return TypeName.BOOLEAN;
    }

    @Override
    public <R> R accept(RuntimeValueVisitor<R> visitor) {
      return visitor.visitBoolean(this);
    }
  }

  record UnitValue() implements RuntimeValue {
    public static final UnitValue INSTANCE = new UnitValue();

    @Override
    public TypeName type() {
      return null;
    }

    @Override
    public <R> R accept(RuntimeValueVisitor<R> visitor) {
      return visitor.visitUnit(this);
    }
  }
}
