package org.printscript.interpreter;

/**
 * Port through which {@code println} writes a line, keeping the interpreter free of any concrete
 * output dependency (stdout, a captured list of lines for tests, ...).
 */
@FunctionalInterface
public interface OutputPort {
  void println(String text);
}
