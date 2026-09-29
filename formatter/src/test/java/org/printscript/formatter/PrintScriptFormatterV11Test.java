package org.printscript.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;

class PrintScriptFormatterV11Test {
  private static final String DECLARES_FLAG_TRUE = "let flag: boolean = true;\n";

  private static final FormatterConfig CONFIG_INDENT_TWO =
      new FormatterConfig(
          Optional.of(0),
          Optional.of(0),
          Optional.of(1),
          Optional.of(1),
          Optional.of(0),
          Optional.of(2),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.of(true));

  private String format(String source, FormatterConfig config) {
    return new PrintScriptFormatter(SpacingRules.v1_1())
        .format(TestSources.programOf(source, KeywordTable.v1_1()), config);
  }

  @Test
  public void movesOpeningBraceOntoSameLineAsIfAndIndentsBlockContent() {
    String source =
        DECLARES_FLAG_TRUE
            + "if (flag)\n"
            + "{\n"
            + "println(\"yes\");\n"
            + "}\n"
            + "else\n"
            + "{\n"
            + "println(\"no\");\n"
            + "}";

    String formatted = format(source, CONFIG_INDENT_TWO);

    assertEquals(
        DECLARES_FLAG_TRUE
            + "if (flag) {\n"
            + "  println(\"yes\");\n"
            + "}\n"
            + "else {\n"
            + "  println(\"no\");\n"
            + "}",
        formatted,
        "expected the brace on the same line as if/else and block content indented by 2 spaces");
  }

  @Test
  public void indentsNestedBlocksByDepthTimesConfiguredSpaces() {
    String source =
        "let a: boolean = true;\n"
            + "let b: boolean = true;\n"
            + "if (a) {\n"
            + "if (b) {\n"
            + "println(\"nested\");\n"
            + "}\n"
            + "}";
    FormatterConfig config =
        new FormatterConfig(
            Optional.of(0),
            Optional.of(0),
            Optional.of(1),
            Optional.of(1),
            Optional.of(0),
            Optional.of(3),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(true));

    String formatted = format(source, config);

    assertEquals(
        "let a: boolean = true;\n"
            + "let b: boolean = true;\n"
            + "if (a) {\n"
            + "   if (b) {\n"
            + "      println(\"nested\");\n"
            + "   }\n"
            + "}",
        formatted,
        "expected nested block content indented by depth times the configured space count");
  }

  @Test
  public void movesOpeningBraceOntoItsOwnLineWhenConfiguredBelow() {
    String source = DECLARES_FLAG_TRUE + "if (flag) {\n" + "  println(\"yes\");\n" + "}";
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
            Optional.empty(),
            Optional.of(false));

    String formatted = format(source, config);

    assertEquals(
        DECLARES_FLAG_TRUE + "if (flag)\n" + "{\n" + "  println(\"yes\");\n" + "}",
        formatted,
        "expected the brace pushed onto its own line below the if, indentation left untouched");
  }
}
