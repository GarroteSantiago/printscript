package org.printscript.application;

/**
 * Port through which {@link PrintScript} reports coarse progress ("Reading statements", "Executing
 * statement", ...) so the core stays independent of how — or whether — a caller renders that. The
 * CLI wires this to a terminal message; {@link #NONE} is the no-op default for callers that don't
 * care.
 */
@FunctionalInterface
public interface ProgressReporter {
  ProgressReporter NONE = message -> {};

  void report(String message);
}
