package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.specs.feature0
import ch.tutteli.atrium.specs.fun1
import org.gradle.testkit.runner.BuildTask

object BuildTaskAssertionsEnSpec: BuildTaskAssertionsSpec(
	feature0(Expect<BuildTask?>::wasInvoked),
	fun1(Expect<BuildTask?>::wasInvoked),
	feature0(Expect<BuildTask?>::wasNotInvoked),
	feature0(Expect<BuildTask?>::wasSuccessful),
	feature0(Expect<BuildTask?>::failed),
	feature0(Expect<BuildTask?>::wasUpToDate),
	feature0(Expect<BuildTask?>::wasSkipped),
	feature0(Expect<BuildTask?>::usedCachedResult),
	feature0(Expect<BuildTask?>::hadNoSource)
)
