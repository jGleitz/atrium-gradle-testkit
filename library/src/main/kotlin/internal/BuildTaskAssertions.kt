package de.joshuagleitze.test.gradle.internal

import ch.tutteli.atrium.core.None
import ch.tutteli.atrium.core.Some
import ch.tutteli.atrium.creating.AssertionContainer
import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import ch.tutteli.atrium.logic._logicAppend
import ch.tutteli.atrium.logic.changeSubject
import ch.tutteli.atrium.logic.manualFeature
import ch.tutteli.atrium.logic.toBe
import ch.tutteli.atrium.reporting.Text
import de.joshuagleitze.test.gradle.internal.BuildTaskDescriptions.INVOKED
import de.joshuagleitze.test.gradle.internal.BuildTaskDescriptions.OUTCOME
import de.joshuagleitze.test.gradle.internal.BuildTaskDescriptions.WAS
import de.joshuagleitze.test.gradle.internal.BuildTaskDescriptions.WAS_NOT
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

internal fun <T : BuildTask?> AssertionContainer<T>.toBeInvoked() = changeSubject.reportBuilder()
	.withDescriptionAndRepresentation(WAS, Text(INVOKED))
	.withTransformation { if (it != null) Some(it) else None }
	.withDefaultFailureHandler()
	.build()

internal fun <T : BuildTask?> AssertionContainer<T>.notToBeInvoked() = changeSubject.reportBuilder()
	.withDescriptionAndRepresentation(WAS_NOT, Text(INVOKED))
	.withTransformation { if (it == null) Some(Unit) else None }
	.withDefaultFailureHandler()
	.build()

internal fun <T : BuildTask> AssertionContainer<T>.outcome() = manualFeature(OUTCOME, BuildTask::getOutcome)

internal fun <T : BuildTask?> AssertionContainer<T>.toHaveOutcome(expected: TaskOutcome): Expect<T & Any> =
	toBeInvoked().transformAndAppend {
		_logic.outcome().transformAndAppend {
			_logicAppend { toBe(expected) }
		}
	}
