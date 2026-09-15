package org.printscript.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.analyzer.NamingStyle;
import org.printscript.formatter.FormatterConfig;
import org.printscript.formatter.FormatterConfigProvider;

public final class JsonPrintScriptConfigReader implements PrintScriptConfigReader {
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public FormatterConfigProvider readFormatterConfig(Path path) throws IOException {
    try (InputStream config = Files.newInputStream(path)) {
      return readFormatterConfig(config);
    }
  }

  @Override
  public AnalyzerConfig readAnalyzerConfig(Path path) throws IOException {
    try (InputStream config = Files.newInputStream(path)) {
      return readAnalyzerConfig(config);
    }
  }

  // Every rule below is Optional.empty() unless the JSON config actually names the key for
  // it -- an absent key must leave that rule's trivia untouched, not fall back to a default.
  // Semicolon spacing is never configured directly (no "spaces-before/after-semicolon"-shaped
  // key exists in this schema), so those two stay permanently empty here.
  public FormatterConfigProvider readFormatterConfig(InputStream config) {
    Map<String, Object> values = readJson(config);
    return new FormatterConfig(
        Optional.empty(),
        Optional.empty(),
        spacesAroundAssignment(values),
        boolKey(values, "mandatory-space-surrounding-operations"),
        intKey(values, "line-breaks-after-println"),
        intKey(values, "indent-inside-if"),
        boolKey(values, "enforce-spacing-before-colon-in-declaration"),
        boolKey(values, "enforce-spacing-after-colon-in-declaration"),
        flag(values, "mandatory-single-space-separation"),
        flag(values, "mandatory-line-break-after-statement"),
        ifBraceOnSameLine(values));
  }

  public AnalyzerConfig readAnalyzerConfig(InputStream config) {
    Map<String, Object> values = readJson(config);

    Object identifierFormat = values.get("identifier_format");
    boolean checkIdentifierNaming = identifierFormat != null;
    NamingStyle namingStyle =
        "camel case".equals(identifierFormat) ? NamingStyle.CAMEL_CASE : NamingStyle.SNAKE_CASE;

    boolean restrictPrintln =
        Boolean.TRUE.equals(values.get("mandatory-variable-or-literal-in-println"));
    boolean restrictReadInput =
        Boolean.TRUE.equals(values.get("mandatory-variable-or-literal-in-readInput"));

    return new AnalyzerConfig(
        namingStyle, checkIdentifierNaming, restrictPrintln, restrictReadInput);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readJson(InputStream config) {
    try {
      return mapper.readValue(config, Map.class);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private Optional<Integer> spacesAroundAssignment(Map<String, Object> values) {
    if (Boolean.TRUE.equals(values.get("enforce-spacing-around-equals"))) {
      return Optional.of(1);
    }
    if (Boolean.TRUE.equals(values.get("enforce-no-spacing-around-equals"))) {
      return Optional.of(0);
    }
    return Optional.empty();
  }

  private Optional<Boolean> ifBraceOnSameLine(Map<String, Object> values) {
    if (Boolean.TRUE.equals(values.get("if-brace-same-line"))) {
      return Optional.of(true);
    }
    if (Boolean.TRUE.equals(values.get("if-brace-below-line"))) {
      return Optional.of(false);
    }
    return Optional.empty();
  }

  // Boolean-flag keys that map to a spaces-count of exactly 1 (the rule is either on or
  // untouched -- this schema never asks for anything other than a single space).
  private Optional<Integer> boolKey(Map<String, Object> values, String key) {
    return Boolean.TRUE.equals(values.get(key)) ? Optional.of(1) : Optional.empty();
  }

  private Optional<Boolean> flag(Map<String, Object> values, String key) {
    return Boolean.TRUE.equals(values.get(key)) ? Optional.of(true) : Optional.empty();
  }

  private Optional<Integer> intKey(Map<String, Object> values, String key) {
    return values.get(key) instanceof Number number
        ? Optional.of(number.intValue())
        : Optional.empty();
  }
}
