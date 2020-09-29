package de.joshuagleitze.test.gradle.translation.en

import ch.tutteli.atrium.reporting.translating.StringBasedTranslatable

enum class BuildResultAssertions(override val value: String): StringBasedTranslatable {
	OUTPUT("output"),
	TASK("task")
}
