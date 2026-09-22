package org.printscript.interpreter;

import java.math.BigDecimal;
import org.printscript.syntax.TypeName;

/**
 * A runtime value produced by evaluating an expression: {@link NumberValue} (arbitrary-precision
 * {@link BigDecimal}, avoiding floating-point surprises), {@link StringValue}, {@link
 * BooleanValue}, or {@link UnitValue} — the "no meaningful value" result of a statement like {@code
 * println}, analogous to Kotlin's {@code Unit} (hence {@link UnitValue#type()} returning {@code
 * null} rather than a real {@link TypeName}). Consumers dispatch through {@link #accept}/ {@link
 * RuntimeValueVisitor} rather than {@code instanceof}.
 */
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
