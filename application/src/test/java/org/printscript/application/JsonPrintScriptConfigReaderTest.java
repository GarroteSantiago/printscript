package org.printscript.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.printscript.analyzer.NamingStyle;
import org.printscript.formatter.FormatterConfigProvider;

class JsonPrintScriptConfigReaderTest {
  @TempDir Path tempDir;

  private final JsonPrintScriptConfigReader reader = new JsonPrintScriptConfigReader();

  @Test
  void readsSpacesAroundAssignmentWhenEnforceEqualsSpacingIsSet() throws Exception {
    FormatterConfigProvider config =
        readFormatterConfig("{\"enforce-spacing-around-equals\": true}");

    assertEquals(Optional.of(1), config.spacesAroundAssignment(), "spacesAroundAssignment");
  }

  @Test
  void readsNoSpacesAroundAssignmentWhenEnforceNoEqualsSpacingIsSet() throws Exception {
    FormatterConfigProvider config =
        readFormatterConfig("{\"enforce-no-spacing-around-equals\": true}");

    assertEquals(Optional.of(0), config.spacesAroundAssignment(), "spacesAroundAssignment");
  }

  @Test
  void leavesSpacesAroundAssignmentUnconfiguredWhenNeitherKeyIsPresent() throws Exception {
    FormatterConfigProvider config = readFormatterConfig("{}");

    assertEquals(
        Optional.empty(), config.spacesAroundAssignment(), "expected an unconfigured rule");
  }

  @Test
  void readsLineBreaksAfterPrintlnAsAnInteger() throws Exception {
    FormatterConfigProvider config = readFormatterConfig("{\"line-breaks-after-println\": 2}");

    assertEquals(Optional.of(2), config.blankLinesBeforePrintln(), "blankLinesBeforePrintln");
  }

  @Test
  void readsIfBraceSameLineAsTrue() throws Exception {
    FormatterConfigProvider config = readFormatterConfig("{\"if-brace-same-line\": true}");

    assertEquals(Optional.of(true), config.ifBraceOnSameLine(), "ifBraceOnSameLine");
  }

  @Test
  void readsIfBraceBelowLineAsFalse() throws Exception {
    FormatterConfigProvider config = readFormatterConfig("{\"if-brace-below-line\": true}");

    assertEquals(Optional.of(false), config.ifBraceOnSameLine(), "ifBraceOnSameLine");
  }

  @Test
  void neverConfiguresSpacesBeforeSemicolon() throws Exception {
    FormatterConfigProvider config =
        readFormatterConfig(
            "{\"enforce-spacing-around-equals\": true, \"if-brace-same-line\": true}");

    assertEquals(Optional.empty(), config.spacesBeforeSemicolon(), "spacesBeforeSemicolon");
  }

  @Test
  void neverConfiguresSpacesAfterSemicolon() throws Exception {
    FormatterConfigProvider config =
        readFormatterConfig(
            "{\"enforce-spacing-around-equals\": true, \"if-brace-same-line\": true}");

    assertEquals(Optional.empty(), config.spacesAfterSemicolon(), "spacesAfterSemicolon");
  }

  @Test
  void readsIdentifierFormatCamelCaseAsTheNamingStyle() throws Exception {
    var config = readAnalyzerConfig("{\"identifier_format\": \"camel case\"}");

    assertEquals(NamingStyle.CAMEL_CASE, config.namingStyle(), "namingStyle");
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts") // false positive: PMD treats the
  // checkIdentifierNaming() accessor call as a second assertion because its name starts with
  // "check"
  void enablesNamingCheckWhenIdentifierFormatIsPresent() throws Exception {
    var config = readAnalyzerConfig("{\"identifier_format\": \"camel case\"}");

    assertTrue(config.checkIdentifierNaming(), "checkIdentifierNaming");
  }

  @Test
  void defaultsToSnakeCaseWhenIdentifierFormatIsAbsent() throws Exception {
    var config = readAnalyzerConfig("{}");

    assertEquals(NamingStyle.SNAKE_CASE, config.namingStyle(), "namingStyle");
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts") // false positive: PMD treats the
  // checkIdentifierNaming() accessor call as a second assertion because its name starts with
  // "check"
  void disablesNamingCheckWhenIdentifierFormatIsAbsent() throws Exception {
    var config = readAnalyzerConfig("{}");

    assertFalse(config.checkIdentifierNaming(), "expected the naming check to stay disabled");
  }

  @Test
  void readsRestrictPrintlnFlag() throws Exception {
    var config = readAnalyzerConfig("{\"mandatory-variable-or-literal-in-println\": true}");

    assertTrue(config.restrictPrintlnToSimpleArguments(), "restrictPrintlnToSimpleArguments");
  }

  @Test
  void readsRestrictReadInputFlag() throws Exception {
    var config = readAnalyzerConfig("{\"mandatory-variable-or-literal-in-readInput\": true}");

    assertTrue(config.restrictReadInputToSimpleArguments(), "restrictReadInputToSimpleArguments");
  }

  @Test
  void leavesRestrictPrintlnFalseWhenAbsent() throws Exception {
    var config = readAnalyzerConfig("{}");

    assertFalse(config.restrictPrintlnToSimpleArguments(), "restrictPrintlnToSimpleArguments");
  }

  @Test
  void leavesRestrictReadInputFalseWhenAbsent() throws Exception {
    var config = readAnalyzerConfig("{}");

    assertFalse(config.restrictReadInputToSimpleArguments(), "restrictReadInputToSimpleArguments");
  }

  @Test
  void readsFormatterConfigFromAFileViaThePathBasedInterface() throws Exception {
    Path configFile = tempDir.resolve("formatter.json");
    Files.writeString(configFile, "{\"enforce-spacing-around-equals\": true}");

    FormatterConfigProvider config = reader.readFormatterConfig(configFile);

    assertEquals(Optional.of(1), config.spacesAroundAssignment(), "spacesAroundAssignment");
  }

  @Test
  void readsAnalyzerConfigFromAFileViaThePathBasedInterface() throws Exception {
    Path configFile = tempDir.resolve("analyzer.json");
    Files.writeString(configFile, "{\"identifier_format\": \"camel case\"}");

    var config = reader.readAnalyzerConfig(configFile);

    assertEquals(NamingStyle.CAMEL_CASE, config.namingStyle(), "namingStyle");
  }

  private FormatterConfigProvider readFormatterConfig(String json) {
    return reader.readFormatterConfig(
        new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  private org.printscript.analyzer.AnalyzerConfig readAnalyzerConfig(String json) {
    return reader.readAnalyzerConfig(
        new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }
}
