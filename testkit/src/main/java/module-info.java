module org.printscript.testkit {
  requires transitive org.printscript.lexer;
  requires transitive org.printscript.ast;
  requires transitive org.printscript.tokens;
  requires org.printscript.parser;

  exports org.printscript.testkit;
}
