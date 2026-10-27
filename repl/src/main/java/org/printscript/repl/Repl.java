package org.printscript.repl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.printscript.ast.StatementSource;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.diagnostics.DiagnosticCode;
import org.printscript.diagnostics.DiagnosticException;
import org.printscript.lexer.KeywordTable;
import org.printscript.lexer.Lexer;
import org.printscript.parser.StatementSyntaxReader;

/**
 * The REPL adapter: a second, thin composition root alongside {@code cli}, reading statements from
 * a {@link Reader} instead of a whole source file. Depends on the language core ({@code lexer},
 * {@code parser}, {@code typechecker}, {@code interpreter}) directly rather than on {@code
 * toolchain.PrintScript} — {@code PrintScript}'s public methods each consume a whole program in one
 * call, which has no natural place to thread state back out between statements; this adapter needs
 * exactly the per-statement primitives ({@code SemanticContext#validate}, {@code
 * Interpreter#executeAll}) those methods already build on internally, so it composes them itself
 * instead of asking {@code toolchain} to grow a new, REPL-specific API. All session state (see
 * {@link ReplSession}) lives here, never in the language core.
 *
 * <p>Reads one line at a time rather than streaming raw characters into a single persistent {@code
 * StatementSyntaxReader}. That would seem simpler — the parser's lookahead would just block for
 * more input on an unfinished multi-line block, "for free" — but {@code StatementSyntaxReader}
 * always reads one token *past* whatever it just matched (its constructor fetches two tokens up
 * front, and every {@code advance()} refills one more), so confirming statement N complete always
 * requires blocking on statement N+1's first token. In practice that means the prompt for N+1
 * doesn't appear, and N's own output doesn't print, until the user has already started typing N+1 —
 * verified by hand, not a hypothetical: every statement's prompt and output land one command late.
 * Buffering a line at a time and parsing the whole buffer after each line avoids this entirely,
 * since every parse then runs over an in-memory, already-fully-read {@link StringReader} with a
 * definite end, never blocking mid-parse.
 *
 * <p>Whether the buffer is finished is the parser's call, not a guess made here from tokens: if
 * parsing fails with {@link DiagnosticCode#UNEXPECTED_END_OF_INPUT} (an unclosed block, a missing
 * {@code ;}, a trailing operator), the REPL shows the continuation prompt and reads another line.
 * Nothing in the buffer runs until all of it parses. A blank line while continuing forces
 * evaluation, reporting the incomplete statement instead of waiting forever. Any other syntax error
 * (a lexical one included) is reported right away.
 */
public final class Repl {
  private static final String PROMPT = "ps> ";
  private static final String CONTINUATION_PROMPT = "... ";

  private Repl() {}

  public static void main(String[] args) {
    boolean v11 = args.length > 0 && "1.1".equals(args[0]);
    run(new InputStreamReader(System.in, StandardCharsets.UTF_8), v11, System.out, System.err);
  }

  /**
   * Drives the loop over an arbitrary {@link Reader}/{@link PrintStream} trio so this is testable
   * with in-memory streams, not just a real terminal.
   */
  public static void run(Reader input, boolean v11, PrintStream out, PrintStream err) {
    KeywordTable keywords = v11 ? KeywordTable.v1_1() : KeywordTable.v1();
    BufferedReader lineReader = new BufferedReader(input);
    ReplSession session = new ReplSession(v11, out::println);
    StringBuilder buffer = new StringBuilder();

    out.print(PROMPT);
    out.flush();

    while (true) {
      String line = readLine(lineReader);
      if (line == null) {
        return;
      }
      boolean continuing = !buffer.isEmpty();
      buffer.append(line).append('\n');

      Parsed parsed = parse(buffer.toString(), keywords);
      boolean forceEvaluation = continuing && line.isBlank();
      if (parsed.endedTooEarly() && !forceEvaluation) {
        out.print(CONTINUATION_PROMPT);
        out.flush();
        continue;
      }

      for (StatementSyntax statement : parsed.statements()) {
        printAll(session.step(statement), err);
      }
      printAll(parsed.syntaxError(), err);
      buffer.setLength(0);
      out.print(PROMPT);
      out.flush();
    }
  }

  private static String readLine(BufferedReader reader) {
    try {
      return reader.readLine();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  /**
   * The statements that parsed from a buffer, followed by the syntax error that stopped parsing (if
   * any). {@link #endedTooEarly} is the parser's own verdict that more input could complete the
   * buffer — never a guess made here from tokens.
   */
  private record Parsed(List<StatementSyntax> statements, List<Diagnostic> syntaxError) {
    boolean endedTooEarly() {
      return syntaxError.stream()
          .anyMatch(diagnostic -> diagnostic.code() == DiagnosticCode.UNEXPECTED_END_OF_INPUT);
    }
  }

  /**
   * Parses all of {@code source}, stopping at the first syntax error. {@code source} is already
   * fully buffered, so this never blocks: a syntax error is a normal, bounded parse failure here,
   * not the state {@link Repl}'s class doc describes needing to recover from.
   */
  private static Parsed parse(String source, KeywordTable keywords) {
    List<StatementSyntax> statements = new ArrayList<>();
    try {
      StatementSource reader =
          new StatementSyntaxReader(new Lexer(new StringReader(source), keywords));
      while (reader.hasNext()) {
        statements.add(reader.next());
      }
      return new Parsed(statements, List.of());
    } catch (DiagnosticException exception) {
      return new Parsed(statements, exception.diagnostics());
    }
  }

  private static void printAll(List<Diagnostic> diagnostics, PrintStream err) {
    for (Diagnostic diagnostic : diagnostics) {
      err.println(render(diagnostic));
    }
  }

  private static String render(Diagnostic diagnostic) {
    var start = diagnostic.span().start();
    var end = diagnostic.span().end();
    return String.format(
        "%s %s at row %d column %d to row %d column %d: %s",
        diagnostic.severity(),
        diagnostic.phase(),
        start.row(),
        start.column(),
        end.row(),
        end.column(),
        diagnostic.message());
  }
}
