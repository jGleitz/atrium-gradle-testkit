package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.api.fluent.en_GB.all
import ch.tutteli.atrium.api.fluent.en_GB.feature
import ch.tutteli.atrium.api.fluent.en_GB.toBe
import ch.tutteli.atrium.api.verbs.internal.expect
import ch.tutteli.atrium.assertions.Assertion
import ch.tutteli.atrium.assertions.AssertionGroup
import ch.tutteli.atrium.assertions.ExplanatoryAssertionGroupType
import ch.tutteli.atrium.assertions.builders.assertionBuilder
import ch.tutteli.atrium.core.None
import ch.tutteli.atrium.creating.CollectingExpect
import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import ch.tutteli.atrium.logic.creating.RootExpectBuilder
import ch.tutteli.atrium.reporting.AtriumErrorAdjuster
import ch.tutteli.atrium.reporting.erroradjusters.NoOpAtriumErrorAdjuster
import io.kotest.core.spec.style.FunSpec

internal fun <T> FunSpec.registerSubjectLessSpec(
	groupPrefix: String,
	vararg assertionCreator: Pair<String, Expect<T>.() -> Unit>
) {
	context("${groupPrefix}assertion function can be used in an ${AssertionGroup::class.simpleName} with an ${ExplanatoryAssertionGroupType::class.simpleName} and report without failure") {
		assertionCreator.forEach { (name, createAssertion) ->
			test("fun `$name`") {
				val assertions = CollectingExpect<T>(None, expect(1)._logic.components)
					.addAssertionsCreatedBy(createAssertion)
					.getAssertions()

				expandAssertionGroups(assertions)


				val container = RootExpectBuilder.forSubject(1.0)
					.withVerb("custom assertion verb")
					.withOptions {
						withComponent(AtriumErrorAdjuster::class) { _ -> NoOpAtriumErrorAdjuster }
					}
					.build()

				val explanatoryGroup = assertionBuilder.explanatoryGroup
					.withDefaultType
					.withAssertions(assertions)
					.build()
				container.addAssertion(explanatoryGroup)
			}
		}
	}

	context("${groupPrefix}assertion function does not hold if there is no subject") {
		assertionCreator.forEach { (name, createAssertion) ->
			test("fun `$name`") {
				val assertions = CollectingExpect<T>(None, expect(1)._logic.components)
					.addAssertionsCreatedBy(createAssertion)
					.getAssertions()
				expect(assertions).all { feature(Assertion::holds).toBe(false) }
			}
		}
	}
}

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
