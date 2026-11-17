package de.joshuagleitze.test.gradle.consumer

import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import ch.tutteli.atrium.creating.Expect
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
 * Compile probes for every public operation and overload. Each probe passes the result of an operation to [exactly]
 * with an explicit type argument. Because `Expect` is invariant, the probes only compile if every operation returns
 * exactly the declared subject type: the original receiver type for creator overloads and the definitely non-null
 * `T & Any` for narrowing operations, for concrete, nullable and generic receivers. All expectations hold.
 */

/** Returns [value] unchanged; an explicit type argument makes the compiler check the type of [value]. */
fun <E> exactly(value: E): E = value

fun probeBuildResultOperations(result: ConcreteBuildResult) {
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

fun <T : BuildResult> probeGenericBuildResult(result: T) {
	exactly<Expect<String>>(expect(result).output).toContain("probe")
	exactly<Expect<T>>(expect(result).output { toContain("probe") })
	exactly<Expect<BuildTask?>>(expect(result).task(":success"))
	exactly<Expect<T>>(expect(result).task(":success") { toBeInvoked() })
	exactly<Expect<T>>(
		expect(result)
			.output { toContain("probe") }
			.task(":success") { toBeSuccessful() }
			.task(":missing") { notToBeInvoked() }
	)
}

fun probeNonNullReceiver(tasks: Map<TaskOutcome, ConcreteBuildTask>) {
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

fun probeNullableReceiver(tasks: Map<TaskOutcome, ConcreteBuildTask?>, missing: ConcreteBuildTask?) {
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

fun <T : BuildTask?> probeGenericReceiver(tasks: Map<TaskOutcome, T>, missing: T) {
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
	if (missing == null) exactly<Expect<Unit>>(expect(missing).notToBeInvoked())
}

fun runSignatureProbes() {
	val tasks = TaskOutcome.entries.associateWith { outcome -> buildTask(":${outcome.name}", outcome) }
	probeBuildResultOperations(buildResult("probe", listOf(buildTask(":success", SUCCESS))))
	probeGenericBuildResult<BuildResult>(buildResult("probe", listOf(buildTask(":success", SUCCESS))))
	probeGenericBuildResult<ConcreteBuildResult>(buildResult("probe", listOf(buildTask(":success", SUCCESS))))
	probeNonNullReceiver(tasks)
	probeNullableReceiver(tasks, missing = null)
	probeGenericReceiver<ConcreteBuildTask>(tasks, tasks.getValue(SUCCESS))
	probeGenericReceiver<ConcreteBuildTask?>(tasks, missing = null)
}
