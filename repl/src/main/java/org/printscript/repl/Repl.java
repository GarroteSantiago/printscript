package org.printscript.repl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.printscript.ast.StatementSource;
import org.printscript.ast.nodes.statements.StatementSyntax;
import org.printscript.diagnostics.Diagnostic;
import org.printscript.lexer.KeywordTable;
import org.printscript.lexer.Lexer;
import org.printscript.parser.StatementSyntaxReader;
import org.printscript.tokens.SyntaxException;
import org.printscript.tokens.Token;
import org.printscript.tokens.TokenSource;
import org.printscript.tokens.TokenType;

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
 * Buffering a line at a time and only parsing once {@link #isOpenBlock} says the buffer looks
 * complete avoids this entirely, since every parse then runs over an in-memory, already-fully-read
 * {@link StringReader} with a definite end, never blocking mid-parse.
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
      buffer.append(line).append('\n');

      if (isOpenBlock(buffer.toString(), keywords)) {
        out.print(CONTINUATION_PROMPT);
        out.flush();
        continue;
      }

      runBuffer(buffer.toString(), keywords, session, err);
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
   * True if {@code source} has an unclosed {@code { }} block and the caller should read another
   * line before attempting a real parse. A lex error here is deliberately treated as "not open" —
   * the real parse in {@link #runBuffer} will hit the identical error and report it, rather than
   * waiting forever for more input that wouldn't fix it.
   */
  private static boolean isOpenBlock(String source, KeywordTable keywords) {
    TokenSource tokens = new Lexer(new StringReader(source), keywords);
    int depth = 0;
    while (true) {
      Token token;
      try {
        token = tokens.next();
      } catch (SyntaxException exception) {
        return false;
      }
      if (token.type() == TokenType.EOF) {
        return depth > 0;
      }
      if (token.type() == TokenType.LEFT_BRACE) {
        depth++;
      } else if (token.type() == TokenType.RIGHT_BRACE) {
        depth--;
      }
    }
  }

  /**
   * Parses and executes {@code source} statement by statement, stopping at the first failure —
   * matching how {@code toolchain.PrintScript.execute} handles a whole file. {@code source} is
   * already fully buffered, so this never blocks: a syntax error is a normal, bounded parse failure
   * here, not the state {@link Repl}'s class doc describes needing to recover from.
   */
  private static void runBuffer(
      String source, KeywordTable keywords, ReplSession session, PrintStream err) {
    StatementSource statements;
    try {
      statements = new StatementSyntaxReader(new Lexer(new StringReader(source), keywords));
    } catch (SyntaxException exception) {
      err.println(render(exception.diagnostic()));
      return;
    }
    while (statements.hasNext()) {
      StatementSyntax statement;
      try {
        statement = statements.next();
      } catch (SyntaxException exception) {
        err.println(render(exception.diagnostic()));
        return;
      }
      List<Diagnostic> diagnostics = session.step(statement);
      for (Diagnostic diagnostic : diagnostics) {
        err.println(render(diagnostic));
      }
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
