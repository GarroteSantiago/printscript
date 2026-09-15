module org.prinstscript.application {
  requires org.printscript.syntax;
  requires org.printscript.semantics;
  requires org.printscript.interpreter;
  requires org.printscript.formatter;
  requires org.printscript.analyzer;
  requires org.printscript.lexer;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;

  exports org.printscript.application;
}
