package org.printscript.interpreter;

@FunctionalInterface
public interface BuiltinBehavior {
  RuntimeValue invoke(RuntimeValue argument, BuiltinRuntimeContext context);
}
