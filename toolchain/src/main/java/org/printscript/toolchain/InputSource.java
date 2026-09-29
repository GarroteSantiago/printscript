package org.printscript.toolchain;

/**
 * Port through which {@code execute} reads an interactive {@code readInput} line, keeping this
 * facade's public API free of any lower module's port types — {@code cli} implements this directly
 * against real stdin, without needing to depend on {@code interpreter} just to supply it. Adapted
 * internally to {@code interpreter.InputPort} when constructing the {@link
 * org.printscript.interpreter.Interpreter} that actually needs it.
 */
@FunctionalInterface
public interface InputSource {
  String readLine(String prompt);
}
