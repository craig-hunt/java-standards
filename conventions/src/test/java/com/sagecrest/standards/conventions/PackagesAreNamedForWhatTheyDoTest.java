package com.sagecrest.standards.conventions;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts that no package is named for a category instead of a job.
 *
 * <p>A package called {@code util} tells a reader nothing, which means nobody can say what belongs
 * in it, which means everything does. The same goes for {@code helper}, {@code common} and {@code
 * misc}. {@code impl} is the other half of the same problem: it names a package after a
 * relationship to another package rather than after what the code does.
 *
 * <p>The Go sibling enforces this rule, and it is the one convention here that costs nothing to
 * follow and is almost never followed without a check.
 */
class PackagesAreNamedForWhatTheyDoTest {

  private static final Set<String> SAYS_NOTHING =
      Set.of(
          "util", "utils", "helper", "helpers", "common", "misc", "shared", "base", "impl", "core");

  private static final String SEGMENT_SEPARATOR = "\\.";
  private static final String NO_PACKAGE = "";
  private static final String MSG_VIOLATION = "%s sits in a package segment named %s";

  private static final int ONE_VIOLATION = 1;

  private static final String OFFENDING_SOURCE =
      """
      package com.example.util;

      class Offender {}
      """;

  private static final String COMPLIANT_SOURCE =
      """
      package com.example.signups;

      class Compliant {}
      """;

  @Test
  @DisplayName("no package segment names a category rather than a job")
  void noPackageSegmentNamesACategory() {
    assertThat(violationsIn(Sources.all()))
        .as("rename the package after what the code in it does")
        .isEmpty();
  }

  @Test
  @DisplayName("the rule finds a package named for a category")
  void theRuleFindsACategoryPackage() {
    assertThat(violationsIn(List.of(StaticJavaParser.parse(OFFENDING_SOURCE))))
        .as("a gate nobody has seen fail is a gate nobody knows works")
        .hasSize(ONE_VIOLATION);
  }

  @Test
  @DisplayName("the rule accepts a package named for what it holds")
  void theRuleAcceptsAJobPackage() {
    assertThat(violationsIn(List.of(StaticJavaParser.parse(COMPLIANT_SOURCE)))).isEmpty();
  }

  private static List<String> violationsIn(List<CompilationUnit> units) {
    List<String> violations = new ArrayList<>();
    for (CompilationUnit unit : units) {
      String declared =
          unit.getPackageDeclaration().map(found -> found.getNameAsString()).orElse(NO_PACKAGE);
      for (String segment : declared.split(SEGMENT_SEPARATOR)) {
        if (SAYS_NOTHING.contains(segment)) {
          violations.add(MSG_VIOLATION.formatted(describe(unit), segment));
        }
      }
    }
    return violations;
  }

  /** A parsed string has no path, so the synthetic sources in this file report their package. */
  private static String describe(CompilationUnit unit) {
    return unit.getStorage().isPresent()
        ? Sources.relativize(unit).toString()
        : unit.getPackageDeclaration().map(found -> found.getNameAsString()).orElse(NO_PACKAGE);
  }
}
