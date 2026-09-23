module org.printscript.testkit {
  requires transitive org.printscript.lexer;
  requires transitive org.printscript.ast;
  requires org.printscript.parser;

  exports org.printscript.testkit;
}
