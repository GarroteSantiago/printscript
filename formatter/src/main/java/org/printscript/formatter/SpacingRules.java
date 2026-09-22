package org.printscript.formatter;

import java.util.Optional;
import org.printscript.tokens.SyntaxToken;
import org.printscript.tokens.TokenType;
import org.printscript.tokens.TokenTypeVisitor;

@FunctionalInterface
public interface SpacingRules {
  Optional<String> leadingTriviaFor(
      SyntaxToken token, SyntaxToken previous, FormatterConfigProvider config);

  static SpacingRules v1() {
    return (token, previous, config) ->
        token.type().accept(new V1PrimaryVisitor(token, previous, config));
  }

  static SpacingRules v1_1() {
    return (token, previous, config) ->
        token.type().accept(new V1_1PrimaryVisitor(token, previous, config));
  }

  private static Optional<String> spaces(Optional<Integer> count) {
    return count.map(n -> " ".repeat(n));
  }

  /**
   * Uses the specific rule when configured; otherwise falls back to a single space if the blanket
   * {@code mandatorySingleSpaceSeparation} rule is on, else leaves the trivia untouched.
   */
  private static Optional<String> spacesOrFallback(
      Optional<Integer> specific, FormatterConfigProvider config) {
    return specific.isPresent() ? spaces(specific) : blanketOnly(config);
  }

  private static Optional<String> blanketOnly(FormatterConfigProvider config) {
    return Boolean.TRUE.equals(config.mandatorySingleSpaceSeparation().orElse(false))
        ? Optional.of(" ")
        : Optional.empty();
  }

  private static Optional<String> lineBreakAfterSemicolon(FormatterConfigProvider config) {
    if (config.spacesAfterSemicolon().isPresent()) {
      return spaces(config.spacesAfterSemicolon()).map(spaces -> "\n" + spaces);
    }
    return Boolean.TRUE.equals(config.mandatoryLineBreakAfterStatement().orElse(false))
        ? Optional.of("\n")
        : Optional.empty();
  }

  private static Optional<String> braceOwnTrivia(FormatterConfigProvider config) {
    return config.ifBraceOnSameLine().map(sameLine -> sameLine ? " " : "\n");
  }

  private static Optional<String> braceContentTrivia(FormatterConfigProvider config) {
    return config.blockIndentSpaces().isPresent() ? Optional.of("\n") : Optional.empty();
  }

  private static boolean hasLineBreak(String trivia) {
    return trivia.indexOf('\n') >= 0;
  }

  /** Visits every {@link TokenType} kind, defaulting unhandled ones to {@link #fallback()}. */
  abstract class DefaultingVisitor implements TokenTypeVisitor<Optional<String>> {
    @Override
    public Optional<String> visitLet() {
      return fallback();
    }

    @Override
    public Optional<String> visitIdentifier() {
      return fallback();
    }

    @Override
    public Optional<String> visitType() {
      return fallback();
    }

    @Override
    public Optional<String> visitNumber() {
      return fallback();
    }

    @Override
    public Optional<String> visitString() {
      return fallback();
    }

    @Override
    public Optional<String> visitColon() {
      return fallback();
    }

    @Override
    public Optional<String> visitSemicolon() {
      return fallback();
    }

    @Override
    public Optional<String> visitEqual() {
      return fallback();
    }

    @Override
    public Optional<String> visitPlus() {
      return fallback();
    }

    @Override
    public Optional<String> visitMinus() {
      return fallback();
    }

    @Override
    public Optional<String> visitStar() {
      return fallback();
    }

    @Override
    public Optional<String> visitSlash() {
      return fallback();
    }

    @Override
    public Optional<String> visitLeftParen() {
      return fallback();
    }

    @Override
    public Optional<String> visitRightParen() {
      return fallback();
    }

    @Override
    public Optional<String> visitLeftBrace() {
      return fallback();
    }

    @Override
    public Optional<String> visitRightBrace() {
      return fallback();
    }

    @Override
    public Optional<String> visitConst() {
      return fallback();
    }

    @Override
    public Optional<String> visitIf() {
      return fallback();
    }

    @Override
    public Optional<String> visitElse() {
      return fallback();
    }

    @Override
    public Optional<String> visitBoolean() {
      return fallback();
    }

    @Override
    public Optional<String> visitComment() {
      return fallback();
    }

    @Override
    public Optional<String> visitEof() {
      return fallback();
    }

    protected abstract Optional<String> fallback();
  }

  /** Rule keyed on the previous token, used once the leading token has no rule of its own. */
  class V1SecondaryVisitor extends DefaultingVisitor {
    protected final SyntaxToken token;
    protected final FormatterConfigProvider config;

    V1SecondaryVisitor(SyntaxToken token, FormatterConfigProvider config) {
      this.token = token;
      this.config = config;
    }

    @Override
    public Optional<String> visitSemicolon() {
      return lineBreakAfterSemicolon(config);
    }

    @Override
    public Optional<String> visitEqual() {
      return spacesOrFallback(config.spacesAroundAssignment(), config);
    }

    @Override
    public Optional<String> visitPlus() {
      return spacesOrFallback(config.spacesAroundOperators(), config);
    }

    @Override
    public Optional<String> visitMinus() {
      return spacesOrFallback(config.spacesAroundOperators(), config);
    }

    @Override
    public Optional<String> visitStar() {
      return spacesOrFallback(config.spacesAroundOperators(), config);
    }

    @Override
    public Optional<String> visitSlash() {
      return spacesOrFallback(config.spacesAroundOperators(), config);
    }

    @Override
    public Optional<String> visitColon() {
      return spacesOrFallback(config.spacesAfterColon(), config);
    }

    @Override
    public Optional<String> visitLeftParen() {
      return blanketOnly(config);
    }

    @Override
    protected Optional<String> fallback() {
      return Optional.of(hasLineBreak(token.leadingTrivia()) ? token.leadingTrivia() : " ");
    }
  }

  /** v1.1 additionally gives the content of a block its own leading-trivia rule. */
  class V1_1SecondaryVisitor extends V1SecondaryVisitor {
    V1_1SecondaryVisitor(SyntaxToken token, FormatterConfigProvider config) {
      super(token, config);
    }

    @Override
    public Optional<String> visitLeftBrace() {
      return braceContentTrivia(config);
    }

    @Override
    public Optional<String> visitRightBrace() {
      return braceContentTrivia(config);
    }
  }

  /** Rule keyed on the token about to be emitted; falls back to the previous token's rule. */
  class V1PrimaryVisitor extends DefaultingVisitor {
    protected final SyntaxToken token;
    protected final SyntaxToken previous;
    protected final FormatterConfigProvider config;

    V1PrimaryVisitor(SyntaxToken token, SyntaxToken previous, FormatterConfigProvider config) {
      this.token = token;
      this.previous = previous;
      this.config = config;
    }

    @Override
    public Optional<String> visitSemicolon() {
      return spaces(config.spacesBeforeSemicolon());
    }

    @Override
    public Optional<String> visitEqual() {
      return spacesOrFallback(config.spacesAroundAssignment(), config);
    }

    @Override
    public Optional<String> visitPlus() {
      return spacesOrFallback(config.spacesAroundOperators(), config);
    }

    @Override
    public Optional<String> visitMinus() {
      return spacesOrFallback(config.spacesAroundOperators(), config);
    }

    @Override
    public Optional<String> visitStar() {
      return spacesOrFallback(config.spacesAroundOperators(), config);
    }

    @Override
    public Optional<String> visitSlash() {
      return spacesOrFallback(config.spacesAroundOperators(), config);
    }

    @Override
    public Optional<String> visitColon() {
      return spacesOrFallback(config.spacesBeforeColon(), config);
    }

    @Override
    public Optional<String> visitRightParen() {
      return blanketOnly(config);
    }

    @Override
    public Optional<String> visitLeftParen() {
      return blanketOnly(config);
    }

    @Override
    protected Optional<String> fallback() {
      return previous.type().accept(secondaryVisitor());
    }

    protected TokenTypeVisitor<Optional<String>> secondaryVisitor() {
      return new V1SecondaryVisitor(token, config);
    }
  }

  /** v1.1 adds `if (...)` spacing and brace-trivia rules on top of v1's. */
  class V1_1PrimaryVisitor extends V1PrimaryVisitor {
    V1_1PrimaryVisitor(SyntaxToken token, SyntaxToken previous, FormatterConfigProvider config) {
      super(token, previous, config);
    }

    @Override
    public Optional<String> visitLeftParen() {
      return TokenType.IF.equals(previous.type()) ? Optional.of(" ") : blanketOnly(config);
    }

    @Override
    public Optional<String> visitLeftBrace() {
      return braceOwnTrivia(config);
    }

    @Override
    public Optional<String> visitRightBrace() {
      return Optional.of("\n");
    }

    @Override
    protected TokenTypeVisitor<Optional<String>> secondaryVisitor() {
      return new V1_1SecondaryVisitor(token, config);
    }
  }
}
