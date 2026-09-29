package org.printscript.toolchain;

import java.util.List;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.diagnostics.Severity;

/**
 * The uniform result shape every {@link PrintScript} operation returns: either a {@code value} (on
 * success) or accumulated {@link Diagnostic}s (on failure) — never both, and never a thrown
 * exception. {@link #isSuccess()} checks severity rather than presence of diagnostics, since a
 * successful {@code analyze} can still carry {@link Severity#WARNING} diagnostics alongside its
 * value.
 */
public record CommandResult<T>(T value, List<Diagnostic> diagnostics) {
  public CommandResult {
    diagnostics = List.copyOf(diagnostics);
  }

  public static <T> CommandResult<T> success(T value) {
    return new CommandResult<>(value, List.of());
  }

  public static <T> CommandResult<T> failure(List<Diagnostic> diagnostics) {
    return new CommandResult<>(null, diagnostics);
  }

  public boolean isSuccess() {
    return diagnostics.stream().noneMatch(d -> d.severity() == Severity.ERROR);
  }
}
