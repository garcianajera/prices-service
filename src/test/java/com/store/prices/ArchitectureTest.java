package com.store.prices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;

/**
 * Enforces the hexagonal architecture rules (T4: C2, C3; ADR-0002). Never weaken a rule to make the build pass.
 */
@AnalyzeClasses(packages = "com.store.prices", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "com.store.prices.domain..";
    private static final String APPLICATION = "com.store.prices.application..";
    private static final String INFRASTRUCTURE = "com.store.prices.infrastructure..";

    @ArchTest
    static final ArchRule dependenciesPointInwards = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Infrastructure").definedBy(INFRASTRUCTURE)
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
            .because("dependencies must point inwards: infrastructure → application → domain (C2)");

    @ArchTest
    static final ArchRule coreIsFrameworkFree = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "javax.persistence..",
                    "org.hibernate..",
                    "tools.jackson..",
                    "com.fasterxml.jackson..")
            .because("domain and application must not depend on any framework, persistence or HTTP code (C3)");

    @ArchTest
    static final ArchRule noFieldInjection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
            .because("only constructor injection is allowed");
}
