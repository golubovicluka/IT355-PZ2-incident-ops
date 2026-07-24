package com.golubovicluka.incident_ops.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.golubovicluka.incident_ops")
class BackendArchitectureTest {

	@ArchTest
	static final ArchRule domainMustNotDependOnFrameworks = noClasses()
			.that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..",
					"jakarta.persistence..",
					"jakarta.servlet..",
					"tools.jackson..",
					"com.fasterxml.jackson..");

	@ArchTest
	static final ArchRule domainMustNotDependOnOuterLayers = noClasses()
			.that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"..application..",
					"..infrastructure..",
					"..web..");

	@ArchTest
	static final ArchRule webMustNotAccessInfrastructureDirectly = noClasses()
			.that().resideInAPackage("..web..")
			.should().dependOnClassesThat().resideInAPackage("..infrastructure..");

	@ArchTest
	static final ArchRule responseDtosMustNotContainPasswordFields = noFields()
			.that().areDeclaredInClassesThat().resideInAPackage("..web..response..")
			.should().haveNameMatching("(?i).*password.*");
}
