package org.printscript.interpreter;

import java.util.Optional;

/**
 * Port through which {@code readEnv} looks up an environment variable, keeping the interpreter free
 * of any concrete dependency on {@link System#getenv}. An absent result becomes a {@link
 * RuntimeFailure} at the {@code readEnv} call site, not here.
 */
@FunctionalInterface
public interface EnvironmentPort {
  Optional<String> get(String name);
}
