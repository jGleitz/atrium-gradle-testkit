package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.specs.feature1
import ch.tutteli.atrium.specs.fun1
import ch.tutteli.atrium.specs.property
import org.gradle.testkit.runner.BuildResult

object BuildResultAssertionsEnSpec: BuildResultAssertionsSpec(
	property(Expect<BuildResult>::output),
	fun1(Expect<BuildResult>::output),
	feature1(Expect<BuildResult>::task)
)
