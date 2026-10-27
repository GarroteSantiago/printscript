package org.printscript.formatter;

import java.util.Optional;

/**
 * Port through which the formatter asks for each spacing/indentation rule's value, one rule at a
 * time, instead of depending on a concrete config source. An empty {@link Optional} means the rule
 * was not configured: the formatter leaves the affected trivia exactly as written instead of
 * applying a default. Implementations decide where a rule's value comes from (TOML, JSON, hardcoded
 * defaults, ...) and can be swapped via dependency injection without the formatter itself changing.
 */
public interface FormatterConfigProvider {
  Optional<Integer> spacesBeforeSemicolon();

  Optional<Integer> spacesAfterSemicolon();

  Optional<Integer> spacesAroundAssignment();

  Optional<Integer> spacesAroundOperators();

  /**
   * Blank lines to put after a statement that is exactly a {@code println(...)} call, before the
   * statement that follows it (the {@code line-breaks-after-println} config key).
   */
  Optional<Integer> blankLinesAfterPrintln();

  Optional<Integer> blockIndentSpaces();

  Optional<Integer> spacesBeforeColon();

  Optional<Integer> spacesAfterColon();

  /**
   * Blanket fallback: when present and {@code true}, any of assignment/operator/colon/paren spacing
   * that has no specific rule configured collapses to exactly one space instead of being left
   * untouched. Never applies to semicolon spacing.
   */
  Optional<Boolean> mandatorySingleSpaceSeparation();

  /** When present and {@code true}, forces a line break after every statement's semicolon. */
  Optional<Boolean> mandatoryLineBreakAfterStatement();

  /**
   * When present, forces an {@code if}/{@code else} block's opening brace onto the same line
   * ({@code true}) or its own line below ({@code false}). Absent leaves the brace's placement
   * exactly as written.
   */
  Optional<Boolean> ifBraceOnSameLine();
}
