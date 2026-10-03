package com.sagecrest.standards.conventions;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts the rule that a literal with a meaning carries a name.
 *
 * <p>Written as a test rather than as a compiler plugin. An Error Prone check would run at compile
 * time, which sounds stronger, but it costs the build a set of {@code --add-exports} flags to reach
 * javac's internals, and it moves the rule somewhere a reader cannot run on its own. A failing test
 * names the file and the line, runs under {@code mvn verify} like everything else, and can be read
 * by somebody who has never written a compiler plugin. The Go sibling reaches the same conclusion
 * and walks its AST from a test.
 *
 * <p>The rule applies to test sources too. A test that repeats {@code "Administrator"} because the
 * code it checks reads a constant is a test that keeps passing after the constant changes.
 */
class LiteralsLiveInNamedConstantsTest {

  /**
   * Values that carry no meaning wherever they appear.
   *
   * <p>Zero and one are counting, not facts: an index starts at zero and a step is one. The empty
   * string is the absence of a string. Naming these would produce {@code ZERO} and {@code EMPTY},
   * which tell a reader nothing the literal did not, and the noise would teach people to ignore the
   * rule. A negative one needs no entry: it parses as a minus applied to one.
   */
  private static final Set<String> MEANINGLESS_NUMBERS = Set.of("0", "1");

  private static final String EMPTY_STRING = "";
  private static final String UNDERSCORE = "_";
  private static final String LONG_SUFFIX = "[lL]$";
  private static final String SERIAL_VERSION_UID = "serialVersionUID";

  /**
   * The assertion methods whose argument is a label rather than a value.
   *
   * <p>The Go sibling excuses the name a subtest is given and the name column of a test table, for
   * the same reason: the string is read by whoever reads the failure, it is written once beside the
   * assertion it describes, and nothing else can drift from it. Naming it would put the sentence
   * one indirection away from the assertion it explains.
   */
  private static final Set<String> DESCRIBES_AN_ASSERTION = Set.of("as", "describedAs");

  private static final int ONE_ARGUMENT = 1;
  private static final int FIRST_ARGUMENT = 0;

  private static final String MSG_VIOLATION = "%s:%d holds the literal %s outside a named constant";
  private static final String MSG_FIX =
      "move each of these into a static final constant whose name says what it means";

  private static final int ONE_VIOLATION = 1;

  private static final String OFFENDING_SOURCE =
      """
      class Offender {
        String plan() {
          return "Enterprise";
        }
      }
      """;

  private static final String COMPLIANT_SOURCE =
      """
      class Compliant {
        private static final String PLAN = "Enterprise";

        String plan() {
          return PLAN;
        }
      }
      """;

  private static final String HIDDEN_IN_SERIAL_VERSION_UID =
      """
      class Offender extends RuntimeException {
        private static final long serialVersionUID = 4815162342L;
      }
      """;

  @Test
  @DisplayName("every literal that means something sits in a named constant")
  void everyMeaningfulLiteralSitsInANamedConstant() {
    List<String> violations = new ArrayList<>();

    for (CompilationUnit unit : Sources.all()) {
      for (Expression literal : unnamedLiteralsIn(unit)) {
        violations.add(
            MSG_VIOLATION.formatted(
                Sources.relativize(unit),
                literal.getBegin().map(position -> position.line).orElseThrow(),
                literal));
      }
    }

    assertThat(violations).as(MSG_FIX).isEmpty();
  }

  @Test
  @DisplayName("the rule finds a literal a method body introduces")
  void theRuleFindsALiteralInAMethodBody() {
    assertThat(unnamedLiteralsIn(StaticJavaParser.parse(OFFENDING_SOURCE)))
        .as("a gate nobody has seen fail is a gate nobody knows works")
        .hasSize(ONE_VIOLATION);
  }

  @Test
  @DisplayName("the rule accepts a literal that already is a named constant")
  void theRuleAcceptsANamedConstant() {
    assertThat(unnamedLiteralsIn(StaticJavaParser.parse(COMPLIANT_SOURCE))).isEmpty();
  }

  @Test
  @DisplayName("a value cannot escape the rule by hiding in serialVersionUID")
  void aValueCannotHideInSerialVersionUid() {
    assertThat(unnamedLiteralsIn(StaticJavaParser.parse(HIDDEN_IN_SERIAL_VERSION_UID)))
        .hasSize(ONE_VIOLATION);
  }

  private static List<Expression> unnamedLiteralsIn(CompilationUnit unit) {
    List<Expression> found = new ArrayList<>();
    found.addAll(unit.findAll(StringLiteralExpr.class));
    found.addAll(unit.findAll(IntegerLiteralExpr.class));
    found.addAll(unit.findAll(LongLiteralExpr.class));
    found.addAll(unit.findAll(DoubleLiteralExpr.class));
    found.addAll(unit.findAll(CharLiteralExpr.class));
    return found.stream().filter(literal -> !excused(literal)).toList();
  }

  private static boolean excused(Expression literal) {
    return meaningless(literal)
        || insideAnnotation(literal)
        || initializesAConstant(literal)
        || describesAnAssertion(literal);
  }

  /** A string handed straight to {@code as} or {@code describedAs} labels the assertion. */
  private static boolean describesAnAssertion(Expression literal) {
    return literal.getParentNode().stream()
        .filter(MethodCallExpr.class::isInstance)
        .map(MethodCallExpr.class::cast)
        .anyMatch(
            call ->
                DESCRIBES_AN_ASSERTION.contains(call.getNameAsString())
                    && call.getArguments().size() == ONE_ARGUMENT
                    && call.getArgument(FIRST_ARGUMENT) == literal);
  }

  private static boolean meaningless(Expression literal) {
    if (literal instanceof StringLiteralExpr text) {
      return text.getValue().equals(EMPTY_STRING);
    }
    if (literal instanceof IntegerLiteralExpr || literal instanceof LongLiteralExpr) {
      return MEANINGLESS_NUMBERS.contains(digitsOf(literal));
    }
    return false;
  }

  private static String digitsOf(Expression literal) {
    return literal
        .toString()
        .replace(UNDERSCORE, EMPTY_STRING)
        .replaceAll(LONG_SUFFIX, EMPTY_STRING);
  }

  /**
   * An annotation's arguments must be compile-time constants, so a literal there has nowhere else
   * to live. A parameterized test's cases are the common instance.
   */
  private static boolean insideAnnotation(Expression literal) {
    return ancestorOfType(literal, AnnotationExpr.class).isPresent();
  }

  /**
   * A literal in the initializer of a {@code static final} field is the named constant this rule
   * asks for.
   *
   * <p>{@code serialVersionUID} does not count as a name. It is the one constant whose identifier
   * is fixed by the platform and says nothing about the value, so allowing it would leave a place
   * to park any number and satisfy the rule.
   */
  private static boolean initializesAConstant(Expression literal) {
    return ancestorOfType(literal, FieldDeclaration.class)
        .filter(FieldDeclaration::isStatic)
        .filter(FieldDeclaration::isFinal)
        .filter(
            declared ->
                declared.getVariables().stream()
                    .noneMatch(variable -> variable.getNameAsString().equals(SERIAL_VERSION_UID)))
        .isPresent();
  }

  /**
   * Walks up to the nearest enclosing node of a given type.
   *
   * <p>JavaParser's own {@code findAncestor} takes the types as varargs, which makes javac create a
   * generic array and warn about it, and this build treats a warning as an error. Walking the
   * parents says the same thing in the same number of lines and needs no suppression.
   */
  private static <T extends Node> Optional<T> ancestorOfType(Node from, Class<T> wanted) {
    Optional<Node> parent = from.getParentNode();
    while (parent.isPresent()) {
      Node node = parent.get();
      if (wanted.isInstance(node)) {
        return Optional.of(wanted.cast(node));
      }
      parent = node.getParentNode();
    }
    return Optional.empty();
  }
}
