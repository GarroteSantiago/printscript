package org.printscript.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.testkit.TestConfigs;
import org.printscript.testkit.TestSources;

class PrintScriptFormatterTest {
  @Test
  public void normalizesControlledSpacingAndPreservesCommentsAndTokenText() {
    String source = "let   a:string='value'; # keep\nprintln(a);";

    String formatted =
        Formatting.format(TestSources.tokensOf(source), TestConfigs.formatterDefaults());

    assertEquals(
        "let a: string = 'value';# keep\nprintln(a);\n", formatted, "expected normalized spacing");
  }

  @Test
  public void insertsBlankLinesBetweenConsecutivePrintlnCalls() {
    String source = "println(\"a\");\nprintln(\"b\");";

    String formatted =
        Formatting.format(
            TestSources.tokensOf(source), TestConfigs.formatterOneBlankLineAfterPrintln());

    assertEquals(
        "println(\"a\");\n\nprintln(\"b\");\n",
        formatted,
        "expected a blank line between two consecutive println calls");
  }

  @Test
  public void doesNotInsertBlankLinesAfterANonPrintlnStatement() {
    String source = "let text: string = \"hello\";\nprintln(text);";

    String formatted =
        Formatting.format(
            TestSources.tokensOf(source), TestConfigs.formatterOneBlankLineAfterPrintln());

    assertEquals(
        "let text: string = \"hello\";\nprintln(text);\n",
        formatted,
        "expected no blank line since the preceding statement was not a println call");
  }

  @Test
  public void writesFormattedStatementsToAppendable() throws Exception {
    var output = new StringWriter();

    new PrintScriptFormatter(SpacingRules.v1())
        .formatAll(
            TestSources.tokensOf("println(\"a\");\nprintln(\"b\");"),
            TestConfigs.formatterOneBlankLineAfterPrintln(),
            output);

    assertEquals(
        "println(\"a\");\n\nprintln(\"b\");\n",
        output.toString(),
        "expected formatted statements written to the appendable");
  }

  @Test
  public void leavesTriviaUntouchedWhenItsRuleIsNotConfigured() {
    String source = "let a: string  =  \"value\";";
    FormatterConfig config =
        new FormatterConfig(
            Optional.of(0),
            Optional.of(0),
            Optional.empty(),
            Optional.of(1),
            Optional.of(0),
            Optional.of(2),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    String formatted = Formatting.format(TestSources.tokensOf(source), config);

    assertEquals(
        "let a: string  =  \"value\";\n",
        formatted,
        "expected the unconfigured assignment spacing to stay exactly as written");
  }

  @Test
  public void enforcesSpacesBeforeColonWithoutTouchingAfterColon() {
    String source = "let something:string = \"value\";";
    FormatterConfig config =
        new FormatterConfig(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    String formatted = Formatting.format(TestSources.tokensOf(source), config);

    assertEquals(
        "let something :string = \"value\";",
        formatted,
        "expected a forced space before the colon and untouched spacing after it");
  }

  @Test
  public void mandatorySingleSpaceSeparationFillsInUnconfiguredRules() {
    String source = "let something:      string=\"value\";\nprintln(something);";
    FormatterConfig config =
        new FormatterConfig(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(true),
            Optional.empty(),
            Optional.empty());

    String formatted = Formatting.format(TestSources.tokensOf(source), config);

    assertEquals(
        "let something : string = \"value\";\nprintln ( something );",
        formatted,
        "expected the blanket single-space rule to cover colon, assignment, and parens");
  }

  @Test
  public void mandatoryLineBreakAfterStatementForcesNewlinesBetweenStatements() {
    var tokens = TestSources.tokensOf("let a: string = \"x\";let b: string = \"y\";");
    FormatterConfig config =
        new FormatterConfig(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(true),
            Optional.empty());

    String formatted = Formatting.format(tokens, config);

    assertEquals(
        "let a: string = \"x\";\nlet b: string = \"y\";",
        formatted,
        "expected a forced line break between statements with no extra spacing");
  }

  @Test
  public void aCallInsideALargerExpressionIsNotTreatedAsAPrintlnStatement() {
    String source = "println(\"a\") + 1;\nprintln(\"b\");";

    String formatted =
        Formatting.format(
            TestSources.tokensOf(source), TestConfigs.formatterOneBlankLineAfterPrintln());

    assertEquals(
        "println(\"a\") + 1;\nprintln(\"b\");\n",
        formatted,
        "expected no blank line, since the first statement is not exactly a println call");
  }

  @Test
  public void formatsSyntacticallyInvalidTokensWithoutValidating() {
    String source = "let x number = 1;";

    String formatted =
        Formatting.format(TestSources.tokensOf(source), TestConfigs.formatterDefaults());

    assertEquals("let x number = 1;\n", formatted, "expected formatting to never validate");
  }
}
