package de.joshuagleitze.test.gradle.consumer

import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import de.joshuagleitze.test.gradle.notToBeInvoked
import de.joshuagleitze.test.gradle.output
import de.joshuagleitze.test.gradle.task
import de.joshuagleitze.test.gradle.toBeInvoked
import de.joshuagleitze.test.gradle.toBeSkipped
import de.joshuagleitze.test.gradle.toBeSuccessful
import de.joshuagleitze.test.gradle.toBeUpToDate
import de.joshuagleitze.test.gradle.toHaveBeenLoadedFromCache
import de.joshuagleitze.test.gradle.toHaveFailed
import de.joshuagleitze.test.gradle.toHaveNoSource
import de.joshuagleitze.test.gradle.toHaveOutcome
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

/*
 * Positive and negative expectations against controlled subjects. Every negative probe must fail with an
 * AssertionError; any other outcome aborts the probe.
 */

private class Probes {
	var checks = 0
		private set

	fun holds(description: String, expectation: () -> Unit) {
		try {
			expectation()
		} catch (error: AssertionError) {
			throw IllegalStateException("Expected to hold, but failed: $description", error)
		}
		checks++
	}

	fun fails(description: String, expectation: () -> Unit) {
		val failed = try {
			expectation()
			false
		} catch (_: AssertionError) {
			true
		}
		check(failed) { "Expected to fail, but held: $description" }
		checks++
	}
}

private val conveniences: Map<TaskOutcome, (BuildTask?) -> Unit> = mapOf(
	SUCCESS to { task -> expect(task).toBeSuccessful() },
	FAILED to { task -> expect(task).toHaveFailed() },
	UP_TO_DATE to { task -> expect(task).toBeUpToDate() },
	SKIPPED to { task -> expect(task).toBeSkipped() },
	FROM_CACHE to { task -> expect(task).toHaveBeenLoadedFromCache() },
	NO_SOURCE to { task -> expect(task).toHaveNoSource() },
)

fun runBehaviorProbes(): Int = with(Probes()) {
	val success = buildTask(":success", SUCCESS)
	val result = buildResult("probe output", listOf(success, buildTask(":failed", FAILED)))

	holds("output feature") { expect(result).output.toContain("probe output") }
	fails("output feature with other content") { expect(result).output.toContain("other") }
	holds("output creator") { expect(result).output { toContain("probe") } }
	fails("output creator with other content") { expect(result).output { toContain("other") } }
	fails("empty output creator") { expect(result).output { } }

	holds("task feature of invoked task") { expect(result).task(":success").toEqual(success) }
	holds("task feature of missing task is present null") { expect(result).task(":missing").toEqual(null) }
	holds("task creator asserting absence") { expect(result).task(":missing") { notToBeInvoked() } }
	fails("task creator asserting presence of missing task") { expect(result).task(":missing") { toBeInvoked() } }
	fails("empty task creator") { expect(result).task(":success") { } }
	holds("creator chaining") {
		expect(result)
			.output { toContain("probe") }
			.task(":success") { toBeSuccessful() }
			.task(":failed") { toHaveFailed() }
			.task(":missing") { notToBeInvoked() }
	}
	fails("creator chaining with one failing link") {
		expect(result)
			.output { toContain("probe") }
			.task(":success") { toHaveFailed() }
			.task(":missing") { notToBeInvoked() }
	}

	holds("toBeInvoked on invoked task") { expect(success).toBeInvoked() }
	fails("toBeInvoked on null") { expect<BuildTask?>(null).toBeInvoked() }
	holds("toBeInvoked creator") { expect<BuildTask?>(success).toBeInvoked { toBeSuccessful() } }
	fails("toBeInvoked creator on null") { expect<BuildTask?>(null).toBeInvoked { toBeSuccessful() } }
	fails("toBeInvoked creator with failing expectation") { expect(success).toBeInvoked { toHaveFailed() } }
	fails("empty toBeInvoked creator") { expect(success).toBeInvoked { } }

	holds("notToBeInvoked on null") { expect<BuildTask?>(null).notToBeInvoked() }
	fails("notToBeInvoked on invoked task") { expect(success).notToBeInvoked() }
	fails("notToBeInvoked on absent upstream subject") { expect(result).task(":success") { toHaveFailed().notToBeInvoked() } }

	holds("toHaveOutcome with matching outcome") { expect(success).toHaveOutcome(SUCCESS) }
	fails("toHaveOutcome with other outcome") { expect(success).toHaveOutcome(FAILED) }
	fails("toHaveOutcome on null") { expect<BuildTask?>(null).toHaveOutcome(SUCCESS) }

	TaskOutcome.entries.forEach { outcome ->
		val task = buildTask(":${outcome.name}", outcome)
		conveniences.forEach { (expected, convenience) ->
			if (expected == outcome) {
				holds("$expected convenience on $outcome") { convenience(task) }
			} else {
				fails("$expected convenience on $outcome") { convenience(task) }
			}
		}
	}
	conveniences.forEach { (expected, convenience) -> fails("$expected convenience on null") { convenience(null) } }

	checks
}
