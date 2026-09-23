package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.printscript.ast.nodes.ProgramSyntax;
import org.printscript.ast.nodes.expressions.LiteralExpressionSyntax;
import org.printscript.ast.nodes.statements.IfStatementSyntax;
import org.printscript.ast.nodes.statements.VariableDeclarationSyntax;
import org.printscript.lexer.KeywordTable;
import org.printscript.testkit.TestSources;
import org.printscript.tokens.SyntaxException;
import org.printscript.types.TypeName;

class ParserV11Test {
  private static final String SOURCE_IF_WITHOUT_ELSE =
      """
      let flag: boolean = true;
      if (flag) {
        println("yes");
      }
      """;

  private static final String SOURCE_IF_WITH_ELSE =
      """
      let flag: boolean = true;
      if (flag) {
        println("yes");
      } else {
        println("no");
      }
      """;

  private ProgramSyntax parse(String source) {
    return TestSources.programOf(source, KeywordTable.v1_1());
  }

  private VariableDeclarationSyntax parseDeclaration(String source) {
    var program = parse(source);
    return assertInstanceOf(
        VariableDeclarationSyntax.class,
        program.statements().getFirst(),
        "expected a variable declaration");
  }

  private TypeName parseInitializerLiteralType(String source) {
    var declaration = parseDeclaration(source);
    var literal =
        assertInstanceOf(
            LiteralExpressionSyntax.class,
            declaration.initializer().orElseThrow(),
            "expected a literal");
    return literal.literalType();
  }

  private IfStatementSyntax parseIfStatement(String source) {
    var program = parse(source);
    return assertInstanceOf(
        IfStatementSyntax.class, program.statements().get(1), "expected an if statement");
  }

  @Test
  void parsesConstDeclarationAsImmutable() {
    var declaration = parseDeclaration("const x: number = 1;");

    assertTrue(declaration.isConst(), "expected a const declaration");
  }

  @Test
  void parsesLetDeclarationAsMutable() {
    var declaration = parseDeclaration("let x: number = 1;");

    assertFalse(declaration.isConst(), "expected a mutable declaration");
  }

  @Test
  void parsesBooleanLiteral() {
    var literalType = parseInitializerLiteralType("let flag: boolean = true;");

    assertEquals(TypeName.BOOLEAN, literalType, "expected a boolean literal type");
  }

  @Test
  void parsesIfWithoutElseHasNoElseBlock() {
    var ifStatement = parseIfStatement(SOURCE_IF_WITHOUT_ELSE);

    assertTrue(ifStatement.elseBlock().isEmpty(), "expected no else block");
  }

  @Test
  void parsesIfWithoutElseHasOneInnerStatement() {
    var ifStatement = parseIfStatement(SOURCE_IF_WITHOUT_ELSE);

    assertEquals(1, ifStatement.thenBlock().statements().size(), "expected one inner statement");
  }

  @Test
  void parsesIfWithElseHasAnElseBlock() {
    var ifStatement = parseIfStatement(SOURCE_IF_WITH_ELSE);

    assertTrue(ifStatement.elseBlock().isPresent(), "expected an else block");
  }

  @Test
  void parsesIfWithElseHasOneElseStatement() {
    var ifStatement = parseIfStatement(SOURCE_IF_WITH_ELSE);

    assertEquals(
        1,
        ifStatement.elseBlock().orElseThrow().statements().size(),
        "expected one else statement");
  }

  @Test
  void rejectsElseIf() {
    String source =
        """
        let flag: boolean = true;
        if (flag) {
          println("yes");
        } else if (flag) {
          println("no");
        }
        """;

    assertThrows(
        SyntaxException.class, () -> parse(source), "expected 'else if' to be a syntax error");
  }

  @Test
  void rejectsIfWithoutBraces() {
    String source =
        """
        let flag: boolean = true;
        if (flag) println("yes");
        """;

    assertThrows(
        SyntaxException.class,
        () -> parse(source),
        "expected a missing block brace to be a syntax error");
  }
}
