package de.joshuagleitze.test.gradle.logic

import ch.tutteli.atrium.creating.AssertionContainer
import ch.tutteli.atrium.logic.manualFeature
import de.joshuagleitze.test.gradle.translation.en.BuildResultAssertions.OUTPUT
import de.joshuagleitze.test.gradle.translation.en.BuildResultAssertions.TASK
import org.gradle.testkit.runner.BuildResult

fun <T : BuildResult> AssertionContainer<T>.output() = manualFeature(OUTPUT, BuildResult::getOutput)

fun <T : BuildResult> AssertionContainer<T>.task(path: String) = manualFeature("$TASK '$path'") { task(path) }
