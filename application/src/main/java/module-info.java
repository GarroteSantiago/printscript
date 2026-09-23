module org.printscript.application {
  requires org.printscript.ast;
  requires org.printscript.parser;
  requires org.printscript.typetable;
  requires org.printscript.typechecker;
  requires transitive org.printscript.interpreter;
  requires transitive org.printscript.formatter;
  requires transitive org.printscript.analyzer;
  requires org.printscript.lexer;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;

  exports org.printscript.application;
}
