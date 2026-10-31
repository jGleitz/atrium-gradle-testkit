package de.joshuagleitze.test.gradle.internal

import ch.tutteli.atrium.creating.AssertionContainer
import ch.tutteli.atrium.logic.manualFeature
import de.joshuagleitze.test.gradle.internal.BuildResultDescriptions.OUTPUT
import de.joshuagleitze.test.gradle.internal.BuildResultDescriptions.TASK
import org.gradle.testkit.runner.BuildResult

internal fun <T : BuildResult> AssertionContainer<T>.output() = manualFeature(OUTPUT, BuildResult::getOutput)

internal fun <T : BuildResult> AssertionContainer<T>.task(path: String) = manualFeature("$TASK '$path'") { task(path) }
