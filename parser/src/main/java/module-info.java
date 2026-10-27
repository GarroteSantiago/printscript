module org.printscript.parser {
  requires org.printscript.diagnostics;
  requires transitive org.printscript.ast;
  requires transitive org.printscript.tokens;

  exports org.printscript.parser;
}
