package org.printscript.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TomlPrintScriptConfigReaderErrorsTest {
  @TempDir Path tempDir;

  @Test
  void throwsWhenTheFormatterSectionIsMissing() throws Exception {
    Path configFile = tempDir.resolve("config.toml");
    Files.writeString(configFile, "[other]\nkey = 1\n");

    assertThrows(
        IllegalArgumentException.class,
        () -> new TomlPrintScriptConfigReader().readFormatterConfig(configFile));
  }

  @Test
  void throwsWhenTheAnalyzerSectionIsMissing() throws Exception {
    Path configFile = tempDir.resolve("config.toml");
    Files.writeString(configFile, "[other]\nkey = 1\n");

    assertThrows(
        IllegalArgumentException.class,
        () -> new TomlPrintScriptConfigReader().readAnalyzerConfig(configFile));
  }

  @Test
  void throwsWhenTheRequiredNamingStyleKeyIsMissing() throws Exception {
    Path configFile = tempDir.resolve("analyzer.toml");
    Files.writeString(
        configFile,
        """
        [analyzer]
        restrict_println_to_simple_arguments = true
        """);

    assertThrows(
        IllegalArgumentException.class,
        () -> new TomlPrintScriptConfigReader().readAnalyzerConfig(configFile));
  }

  @Test
  void parsesAQuotedIntegerValueInTheFormatterSection() throws Exception {
    Path configFile = tempDir.resolve("formatter.toml");
    Files.writeString(
        configFile,
        """
        [formatter]
        spaces_before_semicolon = "1"
        spaces_after_semicolon = 0
        spaces_around_assignment = 1
        spaces_around_operators = 1
        blank_lines_before_println = 0
        """);

    var config = new TomlPrintScriptConfigReader().readFormatterConfig(configFile);

    assertEquals(
        Optional.of(1), config.spacesBeforeSemicolon(), "expected the quoted integer to be parsed");
  }
}
