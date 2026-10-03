package com.sagecrest.standards.conventions;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts that dependencies point inward, and that each layer sees only what it should.
 *
 * <p>Maven's module graph already forbids the obvious reversal: domain declares no dependency on
 * application, so a domain class importing one does not compile. These rules cover what the module
 * graph cannot say. A module graph cannot stop {@code application} from importing Jackson once some
 * other module puts it on the compile classpath transitively, and it cannot stop {@code domain}
 * from reaching into {@code java.sql}, which needs no dependency at all because it ships with the
 * platform.
 */
class DependenciesPointInwardTest {

  private static final String ROOT = "com.sagecrest.standards";

  private static final String DOMAIN = "Domain";
  private static final String APPLICATION = "Application";
  private static final String INFRASTRUCTURE = "Infrastructure";
  private static final String WEB = "Web";

  private static final String DOMAIN_PACKAGES = ROOT + ".domain..";
  private static final String APPLICATION_PACKAGES = ROOT + ".application..";
  private static final String INFRASTRUCTURE_PACKAGES = ROOT + ".infrastructure..";
  private static final String WEB_PACKAGES = ROOT + ".web..";
  private static final String PORTS_PACKAGE = ROOT + ".application.ports";

  private static final String JAVA_PLATFORM = "java..";
  private static final String JAVAX_PLATFORM = "javax..";
  private static final String SQL_PACKAGES = "java.sql..";
  private static final String JACKSON_PACKAGES = "com.fasterxml..";
  private static final String SLF4J_PACKAGES = "org.slf4j..";
  private static final String HTTP_SERVER_PACKAGES = "com.sun.net.httpserver..";
  private static final String HIKARI_PACKAGES = "com.zaxxer..";
  private static final String POSTGRES_PACKAGES = "org.postgresql..";

  private static final String WHY_DOMAIN_IS_PURE =
      "the domain holds rules, and a rule that can reach a database, a clock or a logger is a rule "
          + "no test can pin down";
  private static final String WHY_APPLICATION_IS_FRAMEWORK_FREE =
      "the application declares ports and orchestrates them; a framework type in a signature here "
          + "would travel into the domain's callers and into every test";
  private static final String WHY_NOTHING_IMPORTS_THE_EDGE =
      "the web module wires the others together, so anything importing it would invert the "
          + "direction the whole structure rests on";

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
  }

  @Test
  @DisplayName("each layer depends only on the layers inside it")
  void eachLayerDependsOnlyOnTheLayersInsideIt() {
    layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer(DOMAIN)
        .definedBy(DOMAIN_PACKAGES)
        .layer(APPLICATION)
        .definedBy(APPLICATION_PACKAGES)
        .layer(INFRASTRUCTURE)
        .definedBy(INFRASTRUCTURE_PACKAGES)
        .layer(WEB)
        .definedBy(WEB_PACKAGES)
        .whereLayer(WEB)
        .mayNotBeAccessedByAnyLayer()
        .whereLayer(INFRASTRUCTURE)
        .mayOnlyBeAccessedByLayers(WEB)
        .whereLayer(APPLICATION)
        .mayOnlyBeAccessedByLayers(INFRASTRUCTURE, WEB)
        .whereLayer(DOMAIN)
        .mayOnlyBeAccessedByLayers(APPLICATION, INFRASTRUCTURE, WEB)
        .as(WHY_NOTHING_IMPORTS_THE_EDGE)
        .check(classes);
  }

  @Test
  @DisplayName("the domain reaches nothing but the language")
  void theDomainReachesNothingButTheLanguage() {
    ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage(DOMAIN_PACKAGES)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(DOMAIN_PACKAGES, JAVA_PLATFORM)
            .as(WHY_DOMAIN_IS_PURE);

    rule.check(classes);
  }

  @Test
  @DisplayName("the domain does not reach JDBC, which needs no dependency to reach")
  void theDomainDoesNotReachJdbc() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage(DOMAIN_PACKAGES)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(SQL_PACKAGES, JACKSON_PACKAGES, SLF4J_PACKAGES, HTTP_SERVER_PACKAGES)
        .as(WHY_DOMAIN_IS_PURE)
        .check(classes);
  }

  @Test
  @DisplayName("the application layer takes on no framework, no driver and no logger")
  void theApplicationLayerTakesOnNoFramework() {
    ArchRuleDefinition.classes()
        .that()
        .resideInAPackage(APPLICATION_PACKAGES)
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(APPLICATION_PACKAGES, DOMAIN_PACKAGES, JAVA_PLATFORM)
        .as(WHY_APPLICATION_IS_FRAMEWORK_FREE)
        .check(classes);
  }

  @Test
  @DisplayName("only infrastructure holds the driver and the pool")
  void onlyInfrastructureHoldsTheDriverAndThePool() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideOutsideOfPackages(INFRASTRUCTURE_PACKAGES, WEB_PACKAGES)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(POSTGRES_PACKAGES, HIKARI_PACKAGES, SQL_PACKAGES, JAVAX_PLATFORM)
        .as("a store is the only thing that should know a database exists")
        .check(classes);
  }

  @Test
  @DisplayName("only the web module knows an HTTP request exists")
  void onlyTheWebModuleKnowsAnHttpRequestExists() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideOutsideOfPackage(WEB_PACKAGES)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(HTTP_SERVER_PACKAGES)
        .as("routing, status codes and headers belong to the edge and nowhere else")
        .check(classes);
  }

  @Test
  @DisplayName("a port is an interface, so the layer that owns it cannot also implement it")
  void aPortIsAnInterface() {
    ArchRuleDefinition.classes()
        .that()
        .resideInAPackage(PORTS_PACKAGE)
        .should()
        .beInterfaces()
        .as("a port names what the application needs; a class there would start answering it")
        .check(classes);
  }
}
