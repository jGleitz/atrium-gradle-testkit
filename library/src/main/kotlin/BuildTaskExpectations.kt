package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import de.joshuagleitze.test.gradle.internal.notToBeInvoked
import de.joshuagleitze.test.gradle.internal.toBeInvoked
import de.joshuagleitze.test.gradle.internal.toHaveOutcome
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

/**
 * Expects that the subject of `this` expectation (a [Gradle task][BuildTask]) was invoked. This means that it was
 * either invoked explicitly on the command line, or that it was a (transitive) dependency of another invoked task.
 *
 * @return An [Expect] for the subject of `this` expectation, narrowed to its non-nullable type.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples.toBeInvokedFeature
 */
public fun <T : BuildTask?> Expect<T>.toBeInvoked(): Expect<T & Any> = _logic.toBeInvoked().transform()

/**
 * Expects that the subject of `this` expectation (a [Gradle task][BuildTask]) [was invoked][toBeInvoked] and that it
 * holds all expectations the given [assertionCreator] creates for it.
 *
 * @return An [Expect] for the subject of `this` expectation.
 * @throws AssertionError If [assertionCreator] creates no expectation or if one of its expectations does not hold.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples.toBeInvoked
 */
public fun <T : BuildTask?> Expect<T>.toBeInvoked(assertionCreator: Expect<T & Any>.() -> Unit): Expect<T> =
	_logic.toBeInvoked().collectAndAppend(assertionCreator)

/**
 * Expects that the subject of `this` expectation (a [Gradle task][BuildTask]) was not invoked, that is, that the
 * subject is `null`. This means that the task was neither invoked explicitly on the command line nor a (transitive)
 * dependency of another invoked task. The expectation does not hold if there is no subject, for example because a
 * previous expectation did not hold.
 *
 * @return An [Expect] for [Unit], because there is no task for further expectations.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples.notToBeInvoked
 */
public fun <T : BuildTask?> Expect<T>.notToBeInvoked(): Expect<Unit> = _logic.notToBeInvoked().transform()

/**
 * Expects that the subject of `this` expectation (a [Gradle task][BuildTask]) [was invoked][toBeInvoked] and that its
 * [outcome][BuildTask.getOutcome] is the [expected] one.
 *
 * @return An [Expect] for the subject of `this` expectation, narrowed to its non-nullable type.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples.toHaveOutcome
 */
public fun <T : BuildTask?> Expect<T>.toHaveOutcome(expected: TaskOutcome): Expect<T & Any> =
	_logic.toHaveOutcome(expected)

/**
 * Expects that the subject of `this` expectation (a [Gradle task][BuildTask]) [has the outcome][toHaveOutcome]
 * [SUCCESS].
 *
 * @return An [Expect] for the subject of `this` expectation, narrowed to its non-nullable type.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples.toBeSuccessful
 */
public fun <T : BuildTask?> Expect<T>.toBeSuccessful(): Expect<T & Any> = toHaveOutcome(SUCCESS)

/**
 * Expects that the subject of `this` expectation (a [Gradle task][BuildTask]) [has the outcome][toHaveOutcome]
 * [FAILED].
 *
 * @return An [Expect] for the subject of `this` expectation, narrowed to its non-nullable type.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples.toHaveFailed
 */
public fun <T : BuildTask?> Expect<T>.toHaveFailed(): Expect<T & Any> = toHaveOutcome(FAILED)

/**
 * Expects that the subject of `this` expectation (a [Gradle task][BuildTask]) [has the outcome][toHaveOutcome]
 * [UP_TO_DATE].
 *
 * @return An [Expect] for the subject of `this` expectation, narrowed to its non-nullable type.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples.toBeUpToDate
 */
public fun <T : BuildTask?> Expect<T>.toBeUpToDate(): Expect<T & Any> = toHaveOutcome(UP_TO_DATE)

/**
 * Expects that the subject of `this` expectation (a [Gradle task][BuildTask]) [has the outcome][toHaveOutcome]
 * [SKIPPED].
 *
 * @return An [Expect] for the subject of `this` expectation, narrowed to its non-nullable type.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples.toBeSkipped
 */
public fun <T : BuildTask?> Expect<T>.toBeSkipped(): Expect<T & Any> = toHaveOutcome(SKIPPED)

/**
 * Expects that the subject of `this` expectation (a [Gradle task][BuildTask]) [has the outcome][toHaveOutcome]
 * [FROM_CACHE].
 *
 * @return An [Expect] for the subject of `this` expectation, narrowed to its non-nullable type.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples.toHaveBeenLoadedFromCache
 */
public fun <T : BuildTask?> Expect<T>.toHaveBeenLoadedFromCache(): Expect<T & Any> = toHaveOutcome(FROM_CACHE)

/**
 * Expects that the subject of `this` expectation (a [Gradle task][BuildTask]) [has the outcome][toHaveOutcome]
 * [NO_SOURCE].
 *
 * @return An [Expect] for the subject of `this` expectation, narrowed to its non-nullable type.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples.toHaveNoSource
 */
public fun <T : BuildTask?> Expect<T>.toHaveNoSource(): Expect<T & Any> = toHaveOutcome(NO_SOURCE)
