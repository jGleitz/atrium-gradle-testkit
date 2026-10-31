package de.joshuagleitze.test.gradle.probes

import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import ch.tutteli.atrium.creating.Expect
import de.joshuagleitze.test.gradle.notToBeInvoked
import de.joshuagleitze.test.gradle.output
import de.joshuagleitze.test.gradle.samples.sampleBuildResult
import de.joshuagleitze.test.gradle.samples.sampleBuildTask
import de.joshuagleitze.test.gradle.task
import de.joshuagleitze.test.gradle.toBeInvoked
import de.joshuagleitze.test.gradle.toBeSkipped
import de.joshuagleitze.test.gradle.toBeSuccessful
import de.joshuagleitze.test.gradle.toBeUpToDate
import de.joshuagleitze.test.gradle.toHaveBeenLoadedFromCache
import de.joshuagleitze.test.gradle.toHaveFailed
import de.joshuagleitze.test.gradle.toHaveNoSource
import de.joshuagleitze.test.gradle.toHaveOutcome
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

/*
 * Compile probes for the generic signatures of the public API. Each probe passes the result of an operation to
 * [exactly] with an explicit type argument. Because `Expect` is invariant, the probes only compile if every
 * operation returns exactly the declared subject type, for concrete subtypes as well as for nullable and non-null
 * receivers. The probes are also executed (see ExpectationContractsSpec) with subjects for which all expectations
 * hold.
 */

internal interface ConcreteBuildTask : BuildTask

internal interface ConcreteBuildResult : BuildResult

/** Returns [value] unchanged; an explicit type argument makes the compiler check the type of [value]. */
internal fun <E> exactly(value: E): E = value

internal fun probeBuildResultOperations(result: ConcreteBuildResult) {
	exactly<Expect<String>>(expect(result).output).toContain("probe")
	exactly<Expect<ConcreteBuildResult>>(expect(result).output { toContain("probe") })
	exactly<Expect<BuildTask?>>(expect(result).task(":success"))
	exactly<Expect<ConcreteBuildResult>>(expect(result).task(":success") { exactly<Expect<BuildTask?>>(this).toBeInvoked() })
	exactly<Expect<ConcreteBuildResult>>(
		expect(result)
			.output { toContain("probe") }
			.task(":success") { toBeSuccessful() }
			.task(":missing") { notToBeInvoked() }
	)
	exactly<Expect<BuildTask>>(expect(result).task(":success").toBeInvoked())
	exactly<Expect<BuildTask>>(expect(result).task(":success").toBeSuccessful().toHaveOutcome(SUCCESS))
	exactly<Expect<Unit>>(expect(result).task(":missing").notToBeInvoked())
}

internal fun probeNonNullReceiver(tasks: Map<TaskOutcome, ConcreteBuildTask>) {
	val success = tasks.getValue(SUCCESS)
	exactly<Expect<ConcreteBuildTask>>(expect(success).toBeInvoked())
	exactly<Expect<ConcreteBuildTask>>(expect(success).toBeInvoked { exactly<Expect<ConcreteBuildTask>>(this).toBeSuccessful() })
	exactly<Expect<ConcreteBuildTask>>(expect(success).toHaveOutcome(SUCCESS))
	exactly<Expect<ConcreteBuildTask>>(expect(success).toBeSuccessful().toBeInvoked().toHaveOutcome(SUCCESS))
	exactly<Expect<ConcreteBuildTask>>(expect(tasks.getValue(FAILED)).toHaveFailed())
	exactly<Expect<ConcreteBuildTask>>(expect(tasks.getValue(UP_TO_DATE)).toBeUpToDate())
	exactly<Expect<ConcreteBuildTask>>(expect(tasks.getValue(SKIPPED)).toBeSkipped())
	exactly<Expect<ConcreteBuildTask>>(expect(tasks.getValue(FROM_CACHE)).toHaveBeenLoadedFromCache())
	exactly<Expect<ConcreteBuildTask>>(expect(tasks.getValue(NO_SOURCE)).toHaveNoSource())
}

internal fun probeNullableReceiver(tasks: Map<TaskOutcome, ConcreteBuildTask?>, missing: ConcreteBuildTask?) {
	val success = tasks.getValue(SUCCESS)
	exactly<Expect<ConcreteBuildTask>>(expect(success).toBeInvoked())
	exactly<Expect<ConcreteBuildTask?>>(expect(success).toBeInvoked { exactly<Expect<ConcreteBuildTask>>(this).toBeSuccessful() })
	exactly<Expect<ConcreteBuildTask?>>(expect(success).toBeInvoked { toBeSuccessful() }).toEqual(success)
	exactly<Expect<ConcreteBuildTask>>(expect(success).toHaveOutcome(SUCCESS))
	exactly<Expect<ConcreteBuildTask>>(expect(success).toBeSuccessful())
	exactly<Expect<ConcreteBuildTask>>(expect(tasks.getValue(FAILED)).toHaveFailed())
	exactly<Expect<ConcreteBuildTask>>(expect(tasks.getValue(UP_TO_DATE)).toBeUpToDate())
	exactly<Expect<ConcreteBuildTask>>(expect(tasks.getValue(SKIPPED)).toBeSkipped())
	exactly<Expect<ConcreteBuildTask>>(expect(tasks.getValue(FROM_CACHE)).toHaveBeenLoadedFromCache())
	exactly<Expect<ConcreteBuildTask>>(expect(tasks.getValue(NO_SOURCE)).toHaveNoSource())
	exactly<Expect<Unit>>(expect(missing).notToBeInvoked())
}

internal fun <T : BuildTask?> probeGenericReceiver(tasks: Map<TaskOutcome, T>) {
	val success = tasks.getValue(SUCCESS)
	exactly<Expect<T & Any>>(expect(success).toBeInvoked())
	exactly<Expect<T>>(expect(success).toBeInvoked { exactly<Expect<T & Any>>(this).toBeSuccessful() })
	exactly<Expect<T & Any>>(expect(success).toHaveOutcome(SUCCESS).toBeInvoked())
	exactly<Expect<T & Any>>(expect(success).toBeSuccessful())
	exactly<Expect<T & Any>>(expect(tasks.getValue(FAILED)).toHaveFailed())
	exactly<Expect<T & Any>>(expect(tasks.getValue(UP_TO_DATE)).toBeUpToDate())
	exactly<Expect<T & Any>>(expect(tasks.getValue(SKIPPED)).toBeSkipped())
	exactly<Expect<T & Any>>(expect(tasks.getValue(FROM_CACHE)).toHaveBeenLoadedFromCache())
	exactly<Expect<T & Any>>(expect(tasks.getValue(NO_SOURCE)).toHaveNoSource())
}

internal fun runGenericSignatureProbes() {
	val tasks = TaskOutcome.entries.associateWith { outcome ->
		sampleBuildTask(ConcreteBuildTask::class.java, ":${outcome.name}", outcome)
	}
	probeBuildResultOperations(
		sampleBuildResult(ConcreteBuildResult::class.java, "probe", listOf(sampleBuildTask(":success", SUCCESS)))
	)
	probeNonNullReceiver(tasks)
	probeNullableReceiver(tasks, missing = null)
	probeGenericReceiver<ConcreteBuildTask>(tasks)
	probeGenericReceiver<ConcreteBuildTask?>(tasks)
}
