package org.printscript.toolchain;

import java.util.Arrays;

/**
 * A supported PrintScript language version — a closed set, one singleton per version, so an
 * unsupported version can't be represented at all. {@link #parse} is the one place an arbitrary
 * version string is checked; it rejects anything unsupported up front, which is why {@link
 * PrintScript} has no "unsupported version" path of its own. {@link PrintScript#pipelineFor}
 * dispatches on the version through {@link LanguageVersionVisitor} to pick every per-version
 * strategy for the pipeline.
 */
public interface LanguageVersion {
  <R> R accept(LanguageVersionVisitor<R> visitor);

  LanguageVersion V1_0_0 =
      new LanguageVersion() {
        @Override
        public <R> R accept(LanguageVersionVisitor<R> visitor) {
          return visitor.visitV1_0();
        }

        @Override
        public String toString() {
          return "1.0.0";
        }
      };

  LanguageVersion V1_1_0 =
      new LanguageVersion() {
        @Override
        public <R> R accept(LanguageVersionVisitor<R> visitor) {
          return visitor.visitV1_1();
        }

        @Override
        public String toString() {
          return "1.1.0";
        }
      };

  /**
   * Parses a {@code major.minor} or {@code major.minor.patch} string into a supported version. The
   * patch number never changes the language, so any patch of a supported {@code major.minor} is
   * accepted.
   *
   * @throws IllegalArgumentException if {@code raw} is malformed or names an unsupported version
   */
  static LanguageVersion parse(String raw) {
    String[] parts = raw.split("\\.", -1);
    boolean wellFormed =
        (parts.length == 2 || parts.length == 3)
            && Arrays.stream(parts).allMatch(part -> part.matches("\\d+"));
    if (wellFormed && "1".equals(parts[0]) && "0".equals(parts[1])) {
      return V1_0_0;
    }
    if (wellFormed && "1".equals(parts[0]) && "1".equals(parts[1])) {
      return V1_1_0;
    }
    throw new IllegalArgumentException(
        "Unsupported PrintScript version: " + raw + " (supported: 1.0, 1.1)");
  }
}
