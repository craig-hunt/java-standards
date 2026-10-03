package com.sagecrest.standards.conventions;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts the shape of the types themselves, rather than who depends on whom.
 *
 * <p>These are the rules a reviewer would otherwise have to remember. A class nobody marked final
 * is a class somebody can subclass to work around a rule it enforces; a {@code System.out.println}
 * left in place writes to a stream no log aggregator reads; a checked exception on a domain failure
 * makes every caller in between declare something it cannot act on.
 */
class TypesSayHowTheyAreUsedTest {

  private static final String ROOT = "com.sagecrest.standards";
  private static final String THROWABLE = "java.lang.Throwable";
  private static final String FIELD_STANDARD_OUT = "out";
  private static final String FIELD_STANDARD_ERROR = "err";

  private static final String WHY_FINAL =
      "a class nobody marked final can be subclassed to defeat the rule it was written to enforce; "
          + "mark it final, or make it abstract on purpose";
  private static final String WHY_NO_STANDARD_STREAMS =
      "a line written to standard output carries no level, no timestamp and no request identifier, "
          + "so nothing downstream can filter or correlate it";
  private static final String WHY_UNCHECKED =
      "a checked failure asks every caller between the throw and the edge to declare something it "
          + "cannot act on, and the usual response is a catch that rethrows";
  private static final String WHY_NO_MUTABLE_STATE =
      "a visible mutable static is shared across every request, and with a thread per request there "
          + "is no moment at which it is safe to read";

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
  }

  @Test
  @DisplayName("a concrete class is final, so nothing extends it by accident")
  void aConcreteClassIsFinal() {
    ArchRuleDefinition.classes()
        .that()
        .areNotInterfaces()
        .and()
        .areNotEnums()
        .and()
        .areNotRecords()
        .and()
        .areNotAnnotations()
        .and()
        .doNotHaveModifier(JavaModifier.ABSTRACT)
        .should()
        .haveModifier(JavaModifier.FINAL)
        .as(WHY_FINAL)
        .check(classes);
  }

  @Test
  @DisplayName("every failure this codebase defines is unchecked")
  void everyFailureIsUnchecked() {
    ArchRuleDefinition.classes()
        .that()
        .areAssignableTo(THROWABLE)
        .should()
        .beAssignableTo(RuntimeException.class)
        .as(WHY_UNCHECKED)
        .check(classes);
  }

  @Test
  @DisplayName("nothing writes to standard output or standard error")
  void nothingWritesToStandardStreams() {
    ArchRuleDefinition.noClasses()
        .should()
        .accessField(System.class, FIELD_STANDARD_OUT)
        .orShould()
        .accessField(System.class, FIELD_STANDARD_ERROR)
        .as(WHY_NO_STANDARD_STREAMS)
        .check(classes);
  }

  @Test
  @DisplayName("no visible static field can be reassigned")
  void noVisibleStaticFieldCanBeReassigned() {
    ArchRuleDefinition.fields()
        .that()
        .areStatic()
        .and()
        .areNotPrivate()
        .should()
        .beFinal()
        .as(WHY_NO_MUTABLE_STATE)
        .check(classes);
  }
}
