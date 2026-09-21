package io.github.fereshtehdev.joblens.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the boundary from CLAUDE.md: domain and application code must stay free of
 * Spring, Jackson, HTTP client and adapter types, so it can be unit-tested without a
 * framework and swapped freely behind its ports.
 */
class HexagonalArchitectureTest {

    private static final String[] INNER_PACKAGES = {
            "..analysis.domain..", "..analysis.application..", "..profile.domain.."
    };

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter().importPackages("io.github.fereshtehdev.joblens");
    }

    @Test
    void innerLayersMustNotDependOnSpring() {
        rule("org.springframework..").check(classes);
    }

    @Test
    void innerLayersMustNotDependOnJackson() {
        rule("com.fasterxml.jackson..", "tools.jackson..").check(classes);
    }

    @Test
    void innerLayersMustNotDependOnHttpClientTypes() {
        rule("java.net.http..").check(classes);
    }

    @Test
    void innerLayersMustNotDependOnAdapters() {
        rule("..analysis.adapter..", "..profile.adapter..").check(classes);
    }

    private static ArchRule rule(String... forbiddenPackages) {
        return noClasses()
                .that().resideInAnyPackage(INNER_PACKAGES)
                .should().dependOnClassesThat().resideInAnyPackage(forbiddenPackages);
    }
}
