package org.printscript.toolchain;

/**
 * One method per supported {@link LanguageVersion}. Adding a version adds a method here, which
 * fails compilation in every visitor until it handles the new version.
 */
public interface LanguageVersionVisitor<R> {
  R visitV1_0();

  R visitV1_1();
}
