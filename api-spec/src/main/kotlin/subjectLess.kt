package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.api.fluent.en_GB.feature
import ch.tutteli.atrium.api.fluent.en_GB.notToThrow
import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toHaveElementsAndAll
import ch.tutteli.atrium.api.verbs.expect
import ch.tutteli.atrium.assertions.Assertion
import ch.tutteli.atrium.assertions.AssertionGroup
import ch.tutteli.atrium.assertions.ExplanatoryAssertionGroupType
import ch.tutteli.atrium.assertions.builders.assertionBuilder
import ch.tutteli.atrium.core.None
import ch.tutteli.atrium.core.ExperimentalNewExpectTypes
import ch.tutteli.atrium.creating.CollectingExpect
import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.creating.ExperimentalComponentFactoryContainer
import ch.tutteli.atrium.creating.build
import ch.tutteli.atrium.logic._logic
import ch.tutteli.atrium.logic.creating.RootExpectBuilder
import ch.tutteli.atrium.reporting.AtriumErrorAdjuster
import ch.tutteli.atrium.reporting.Reporter
import ch.tutteli.atrium.reporting.erroradjusters.NoOpAtriumErrorAdjuster
import io.kotest.core.spec.style.FunSpec

/**
 * One assertion function that is checked without a subject.
 *
 * @property name the name the case is reported under.
 * @property reportFragments the semantic description fragments that reporting the case’s assertions must contain.
 * @property createAssertion creates the assertions of the checked assertion function.
 */
internal data class SubjectLessCase<T>(
	val name: String,
	val reportFragments: List<String>,
	val createAssertion: Expect<T>.() -> Unit
)

/**
 * Creates an assertion container that owns the components (reporter, error adjuster, …) used by the subject-less
 * checks. This is the project’s own, minimal replacement for atrium-specs’ subject-less test setup and uses only
 * supported public API: [RootExpectBuilder] from `atrium-logic` is Atrium’s documented entry point for creating
 * custom expectation verbs.
 */
@OptIn(ExperimentalNewExpectTypes::class, ExperimentalComponentFactoryContainer::class)
private fun newAssertionContainer(): Expect<Double> = RootExpectBuilder.forSubject(1.0)
	.withVerb("custom assertion verb")
	.withOptions {
		// keep stack traces untouched so that reporting cannot fail because of this test's own call stack
		withComponent(AtriumErrorAdjuster::class) { _ -> NoOpAtriumErrorAdjuster }
	}
	.build()

/**
 * Collects the assertions that [createAssertion] creates for a subject that is *absent*, i.e. for a previous
 * subject transformation that could not be carried out ([None]). This is not the same as a subject that is present
 * but `null`.
 */
@OptIn(ExperimentalComponentFactoryContainer::class)
internal fun <T> collectAssertionsWithoutSubject(createAssertion: Expect<T>.() -> Unit): List<Assertion> =
	CollectingExpect<T>(None, newAssertionContainer()._logic.components)
		.appendAsGroup(createAssertion)
		.getAssertions()

/**
 * Puts the given [assertions] into an [AssertionGroup] of the [ExplanatoryAssertionGroupType], adds that group to an
 * assertion container and reports it. Returns the report.
 */
@OptIn(ExperimentalComponentFactoryContainer::class)
internal fun reportInExplanatoryGroup(assertions: List<Assertion>): String {
	expandAssertionGroups(assertions)
	val container = newAssertionContainer()
	val explanatoryGroup = assertionBuilder.explanatoryGroup
		.withDefaultType
		.withAssertions(assertions)
		.build()
	container._logic.append(explanatoryGroup)
	val report = StringBuilder()
	container._logic.components.build<Reporter>().format(explanatoryGroup, report)
	return report.toString()
}

/** Calls recursively [AssertionGroup.assertions] on every assertion group contained in [assertions]. */
private tailrec fun expandAssertionGroups(assertions: List<Assertion>) {
	if (assertions.isEmpty()) return

	expandAssertionGroups(
		assertions
			.asSequence()
			.filterIsInstance<AssertionGroup>()
			.flatMap { it.assertions.asSequence() }
			.toList()
	)
}

/**
 * Registers, for every one of the given [cases], the two checks that apply to an assertion function that is used
 * without a subject: its assertions can be reported in an explanatory group without failing, and they do not hold.
 */
internal fun <T> FunSpec.registerSubjectLessTests(cases: List<SubjectLessCase<T>>) {
	context("without a subject") {
		cases.forEach { (name, reportFragments, createAssertion) ->
			test("`$name` can be reported in an explanatory assertion group without failing") {
				val assertions = collectAssertionsWithoutSubject(createAssertion)
				var report = ""
				expect { report = reportInExplanatoryGroup(assertions) }.notToThrow()
				reportFragments.forEach { fragment -> expect(report).toContain(fragment) }
			}

			test("`$name` does not hold") {
				expect(collectAssertionsWithoutSubject(createAssertion)).toHaveElementsAndAll { feature(Assertion::holds).toEqual(false) }
			}
		}
	}
}
