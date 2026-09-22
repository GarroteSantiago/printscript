package org.printscript.interpreter;

/**
 * Port through which {@code readInput} reads a line, keeping the interpreter free of any concrete
 * I/O dependency (stdin, a test double, ...). The CLI's implementation prints {@code prompt} and
 * reads a line from the terminal; contexts with no interactive I/O (formatting, analysis,
 * validation) wire in an implementation that always throws {@link UnsupportedOperationException}.
 */
@FunctionalInterface
public interface InputPort {
  String readLine(String prompt);
}
