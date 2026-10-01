package com.dairy.apipinal.aicopilot.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class AICopilotArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.dairy.apipinal");

    @Test
    void business_modules_should_not_depend_on_aicopilot() {
        noClasses()
                .that().resideInAnyPackage("..animal..", "..production..", "..nutrition..", "..health..", "..reproduction..", "..finance..", "..anomaly..")
                .should().dependOnClassesThat().resideInAPackage("..aicopilot..")
                .check(classes);
    }

    @Test
    void aicopilot_should_not_depend_on_business_repositories() {
        noClasses()
                .that().resideInAPackage("..aicopilot..")
                .should().dependOnClassesThat(
                        com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage("com.dairy.apipinal.animal..", "com.dairy.apipinal.production..", "com.dairy.apipinal.nutrition..", "com.dairy.apipinal.health..", "com.dairy.apipinal.reproduction..", "com.dairy.apipinal.finance..", "com.dairy.apipinal.anomaly..")
                                .and(com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith("Repository"))
                )
                .check(classes);
    }
}
