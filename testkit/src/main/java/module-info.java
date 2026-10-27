module org.printscript.testkit {
  requires transitive org.printscript.lexer;
  requires transitive org.printscript.ast;
  requires transitive org.printscript.tokens;
  requires transitive org.printscript.analyzer;
  requires transitive org.printscript.formatter;
  requires org.printscript.parser;

  exports org.printscript.testkit;
}
