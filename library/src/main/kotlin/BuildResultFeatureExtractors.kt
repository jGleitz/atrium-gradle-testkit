package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import de.joshuagleitze.test.gradle.internal.output
import de.joshuagleitze.test.gradle.internal.task
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask

/**
 * Creates an [Expect] for the [output][BuildResult.getOutput] of the subject of `this` expectation, so that further
 * fluent calls are made on it.
 *
 * @return The newly created [Expect] for the [output][BuildResult.getOutput].
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildResultFeatureExtractorSamples.outputFeature
 */
public val <T : BuildResult> Expect<T>.output: Expect<String>
	get() = _logic.output().transform()

/**
 * Expects that the [output][BuildResult.getOutput] of the subject of `this` expectation holds all expectations the
 * given [assertionCreator] creates for it.
 *
 * @return An [Expect] for the subject of `this` expectation.
 * @throws AssertionError If [assertionCreator] creates no expectation or if one of its expectations does not hold.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildResultFeatureExtractorSamples.output
 */
public fun <T : BuildResult> Expect<T>.output(assertionCreator: Expect<String>.() -> Unit): Expect<T> =
	_logic.output().collectAndAppend(assertionCreator)

/**
 * Creates an [Expect] for the [task][BuildTask] with the given [path] of the subject of `this` expectation, so that
 * further fluent calls are made on it. The subject is `null` if the build did not invoke the task.
 *
 * _Note_: Just like [BuildResult.task], this function expects a *task path*, which must start with a colon.
 *
 * @return The newly created [Expect] for the [task][BuildTask] with the given [path], or for `null` if the build did
 *   not invoke it.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildResultFeatureExtractorSamples.taskFeature
 */
public fun <T : BuildResult> Expect<T>.task(path: String): Expect<BuildTask?> = _logic.task(path).transform()

/**
 * Expects that the [task][BuildTask] with the given [path] of the subject of `this` expectation holds all
 * expectations the given [assertionCreator] creates for it. The subject of [assertionCreator] is `null` if the build
 * did not invoke the task, so that [assertionCreator] can also expect that the task [was not invoked][notToBeInvoked].
 *
 * _Note_: Just like [BuildResult.task], this function expects a *task path*, which must start with a colon.
 *
 * @return An [Expect] for the subject of `this` expectation.
 * @throws AssertionError If [assertionCreator] creates no expectation or if one of its expectations does not hold.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildResultFeatureExtractorSamples.task
 */
public fun <T : BuildResult> Expect<T>.task(path: String, assertionCreator: Expect<BuildTask?>.() -> Unit): Expect<T> =
	_logic.task(path).collectAndAppend(assertionCreator)
