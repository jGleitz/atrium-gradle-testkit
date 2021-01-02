package de.joshuagleitze.test.gradle.logic

import ch.tutteli.atrium.core.None
import ch.tutteli.atrium.core.Some
import ch.tutteli.atrium.creating.AssertionContainer
import ch.tutteli.atrium.logic._logic
import ch.tutteli.atrium.logic._logicAppend
import ch.tutteli.atrium.logic.changeSubject
import ch.tutteli.atrium.logic.manualFeature
import ch.tutteli.atrium.logic.toBe
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions.INVOKED
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions.OUTCOME
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions.WAS
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions.WAS_NOT
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

fun AssertionContainer<BuildTask?>.wasInvoked() = changeSubject.reportBuilder()
	.withDescriptionAndRepresentation(WAS, INVOKED)
	.withTransformation { if (it != null) Some(it) else None }
	.withDefaultFailureHandler()
	.build()

fun AssertionContainer<BuildTask?>.wasNotInvoked() = changeSubject.reportBuilder()
	.withDescriptionAndRepresentation(WAS_NOT, INVOKED)
	.withTransformation { if (it == null) Some(Unit) else None }
	.withDefaultFailureHandler()
	.build()

fun AssertionContainer<BuildTask>.outcome() = manualFeature(OUTCOME) { outcome }

fun AssertionContainer<BuildTask?>.hadOutcome(outcome: TaskOutcome) =
	wasInvoked().transformAndAppend {
		_logic.outcome().transformAndAppend {
			_logicAppend { toBe(outcome) }
		}
	}

