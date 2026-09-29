package org.printscript.application;

import java.util.Optional;

/**
 * Port through which {@code execute} reads an interactive {@code readEnv} lookup, keeping this
 * facade's public API free of any lower module's port types — {@code cli} implements this directly
 * against {@link System#getenv}, without needing to depend on {@code interpreter} just to supply
 * it. Adapted internally to {@code interpreter.EnvironmentPort} when constructing the {@link
 * org.printscript.interpreter.Interpreter} that actually needs it.
 */
@FunctionalInterface
public interface EnvironmentSource {
  Optional<String> get(String name);
}
