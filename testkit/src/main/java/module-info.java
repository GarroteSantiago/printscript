module org.printscript.testkit {
  requires transitive org.printscript.lexer;
  requires transitive org.printscript.syntax;
  requires org.printscript.parser;

  exports org.printscript.testkit;
}
