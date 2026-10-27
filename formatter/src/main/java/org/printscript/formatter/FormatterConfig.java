package org.printscript.formatter;

import java.util.Optional;

public record FormatterConfig(
    Optional<Integer> spacesBeforeSemicolon,
    Optional<Integer> spacesAfterSemicolon,
    Optional<Integer> spacesAroundAssignment,
    Optional<Integer> spacesAroundOperators,
    Optional<Integer> blankLinesAfterPrintln,
    Optional<Integer> blockIndentSpaces,
    Optional<Integer> spacesBeforeColon,
    Optional<Integer> spacesAfterColon,
    Optional<Boolean> mandatorySingleSpaceSeparation,
    Optional<Boolean> mandatoryLineBreakAfterStatement,
    Optional<Boolean> ifBraceOnSameLine)
    implements FormatterConfigProvider {

  public FormatterConfig {
    spacesBeforeSemicolon = clamp(spacesBeforeSemicolon, 0, 1);
    spacesAfterSemicolon = clamp(spacesAfterSemicolon, 0, 1);
    spacesAroundAssignment = clamp(spacesAroundAssignment, 0, 1);
    spacesAroundOperators = clamp(spacesAroundOperators, 0, 1);
    blankLinesAfterPrintln = clamp(blankLinesAfterPrintln, 0, 2);
    blockIndentSpaces = clamp(blockIndentSpaces, 0, 16);
    spacesBeforeColon = clamp(spacesBeforeColon, 0, 1);
    spacesAfterColon = clamp(spacesAfterColon, 0, 1);
  }

  private static Optional<Integer> clamp(Optional<Integer> value, int min, int max) {
    return value.map(v -> Math.max(min, Math.min(max, v)));
  }
}
