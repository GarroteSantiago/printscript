module org.printscript.parser {
  requires org.printscript.diagnostics;
  requires org.printscript.source;
  requires org.printscript.types;
  requires transitive org.printscript.ast;
  requires transitive org.printscript.tokens;

  exports org.printscript.parser;
}
