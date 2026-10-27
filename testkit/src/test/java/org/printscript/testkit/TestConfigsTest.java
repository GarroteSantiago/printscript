package org.printscript.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.analyzer.NamingStyle;

class TestConfigsTest {
  @Test
  public void formatterDefaultsLeavesNoBlankLineAfterPrintln() {
    assertEquals(
        Optional.of(0),
        TestConfigs.formatterDefaults().blankLinesAfterPrintln(),
        "expected no blank line after println by default");
  }

  @Test
  public void formatterOneBlankLineAfterPrintlnSetsOneBlankLine() {
    assertEquals(
        Optional.of(1),
        TestConfigs.formatterOneBlankLineAfterPrintln().blankLinesAfterPrintln(),
        "expected one blank line after println");
  }

  @Test
  public void analyzerDefaultsEnablesEveryRuleWithSnakeCase() {
    assertEquals(
        new AnalyzerConfig(NamingStyle.SNAKE_CASE, true, true, true),
        TestConfigs.analyzerDefaults(),
        "expected every analyzer rule on, with snake_case naming");
  }
}
