package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import de.joshuagleitze.test.gradle.logic.output
import de.joshuagleitze.test.gradle.logic.task
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask

/**
 * Creates an [Expect] for the [output][BuildResult.getOutput] of the subject of this assertion.
 *
 * @return The newly created [Expect] for the [output][BuildResult.getOutput] of the this assertion’s [BuildResult].
 */
public val Expect<BuildResult>.output: Expect<String>
	get() = _logic.output().transform()

/**
 * Adds all assertions created by [assertionCreator] to the [Expect] for the for the [output][BuildResult.getOutput] of the subject of this
 * assertion (a [BuildResult]).
 *
 * @return An [Expect] for the current subject of the assertion.
 * @throws AssertionError If [assertionCreator] creates no assertions or throws an [AssertionError] itself.
 */
public fun Expect<BuildResult>.output(assertionCreator: Expect<String>.() -> Unit): Expect<BuildResult> =
	_logic.output().collectAndAppend(assertionCreator)

/**
 * Creates an [Expect] for the [task][BuildTask] with the given [path] of the subject of this assertion.
 *
 * _Note_: Just like [BuildResult.task], this function expects a *task path*, which must start with a colon.
 *
 * @return The newly created [Expect] for the [task][BuildTask] named “[path]” of the this assertion’s [BuildResult].
 */
public fun Expect<BuildResult>.task(path: String): Expect<BuildTask?> = _logic.task(path).transform()
