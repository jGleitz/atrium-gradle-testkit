package de.joshuagleitze.test.gradle.logic

import ch.tutteli.atrium.creating.AssertionContainer
import ch.tutteli.atrium.logic.manualFeature
import de.joshuagleitze.test.gradle.translation.en.BuildResultAssertions.OUTPUT
import de.joshuagleitze.test.gradle.translation.en.BuildResultAssertions.TASK
import org.gradle.testkit.runner.BuildResult

fun AssertionContainer<BuildResult>.output() = manualFeature(OUTPUT) { output }

fun AssertionContainer<BuildResult>.task(path: String) = manualFeature("${TASK.value} '$path'") { task(path) }
