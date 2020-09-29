package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import de.joshuagleitze.test.gradle.logic.hadOutcome
import de.joshuagleitze.test.gradle.logic.wasInvoked
import de.joshuagleitze.test.gradle.logic.wasNotInvoked
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

/**
 * Expects that the subject of the assertion (a [Gradle task][BuildTask]) was invoked. This means that it was either invoked explicitly in
 * the command line, or that it was a (transitive) dependency of another task that was explicitly invoked in the command line.
 * @return An [Expect] with the non-nullable type [BuildTask] (was `BuildTask?` before).
 */
public fun Expect<BuildTask?>.wasInvoked(): Expect<BuildTask> = _logic.wasInvoked().getExpectOfFeature()

/**
 * Expects that the subject of the assertion (a [Gradle task][BuildTask]) was invoked and adds all assertions created by [assertionCreator] to the
 * [Expect] for the for the subject. A task was invoked iff it was either invoked explicitly in the command line, or it was a (transitive)
 * dependency of another task that was explicitly invoked in the command line.
 *
 * @return An [Expect] for the current subject of the assertion.
 * @throws AssertionError If [assertionCreator] creates no assertions or throws an [AssertionError] itself.
 */
public fun Expect<BuildTask?>.wasInvoked(assertionCreator: Expect<BuildTask>.() -> Unit): Expect<BuildTask?> =
	_logic.wasInvoked().addToInitial(assertionCreator)

/**
 * Expects that the subject of the assertion (a [Gradle task][BuildTask]) was not invoked. This means that it was not invoked explicitly in the
 * command line, and that it was not a (transitive) dependency of any other task that was explicitly invoked in the command line.
 * @return An (unusable) [Expect] for [Unit] because there is no task to execute further assertions on.
 */
public fun Expect<BuildTask?>.wasNotInvoked(): Expect<Unit> = _logic.wasNotInvoked().getExpectOfFeature()

/**
 * Expects that the subject of the assertion (a [Gradle task][BuildTask]) [was invoked][wasInvoked] and that its [outcome][BuildTask.getOutcome] was
 * [SUCCESS].
 * @return An [Expect] with the non-nullable type [BuildTask] (was `BuildTask?` before).
 */
public fun Expect<BuildTask?>.wasSuccessful(): Expect<BuildTask> = _logic.hadOutcome(SUCCESS)

/**
 * Expects that the subject of the assertion (a [Gradle task][BuildTask]) [was invoked][wasInvoked] and that its [outcome][BuildTask.getOutcome] was
 * [FAILED].
 * @return An [Expect] with the non-nullable type [BuildTask] (was `BuildTask?` before).
 */
public fun Expect<BuildTask?>.failed(): Expect<BuildTask> = _logic.hadOutcome(FAILED)

/**
 * Expects that the subject of the assertion (a [Gradle task][BuildTask]) [was invoked][wasInvoked] and that its [outcome][BuildTask.getOutcome] was
 * [UP_TO_DATE].
 * @return An [Expect] with the non-nullable type [BuildTask] (was `BuildTask?` before).
 */
public fun Expect<BuildTask?>.wasUpToDate(): Expect<BuildTask> = _logic.hadOutcome(UP_TO_DATE)

/**
 * Expects that the subject of the assertion (a [Gradle task][BuildTask]) [was invoked][wasInvoked] and that its [outcome][BuildTask.getOutcome] was
 * [SKIPPED].
 * @return An [Expect] with the non-nullable type [BuildTask] (was `BuildTask?` before).
 */
public fun Expect<BuildTask?>.wasSkipped(): Expect<BuildTask> = _logic.hadOutcome(SKIPPED)

/**
 * Expects that the subject of the assertion (a [Gradle task][BuildTask]) [was invoked][wasInvoked] and that its [outcome][BuildTask.getOutcome] was
 * [FROM_CACHE].
 * @return An [Expect] with the non-nullable type [BuildTask] (was `BuildTask?` before).
 */
public fun Expect<BuildTask?>.usedCachedResult(): Expect<BuildTask> = _logic.hadOutcome(FROM_CACHE)

/**
 * Expects that the subject of the assertion (a [Gradle task][BuildTask]) [was invoked][wasInvoked] and that its [outcome][BuildTask.getOutcome] was
 * [NO_SOURCE].
 * @return An [Expect] with the non-nullable type [BuildTask] (was `BuildTask?` before).
 */
public fun Expect<BuildTask?>.hadNoSource(): Expect<BuildTask> = _logic.hadOutcome(NO_SOURCE)
