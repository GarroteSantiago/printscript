module org.printscript.toolchain {
  requires transitive org.printscript.diagnostics;
  requires org.printscript.parser;
  requires org.printscript.typechecker;
  requires transitive org.printscript.interpreter;
  requires transitive org.printscript.formatter;
  requires transitive org.printscript.analyzer;
  requires org.printscript.lexer;
  requires com.fasterxml.jackson.databind;

  exports org.printscript.toolchain;
}
