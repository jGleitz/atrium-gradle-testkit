package de.joshuagleitze.test.gradle.translation.en

import ch.tutteli.atrium.reporting.translating.StringBasedTranslatable

enum class BuildTaskAssertions(override val value: String): StringBasedTranslatable {
	WAS("was"),
	WAS_NOT("was not"),
	INVOKED("invoked"),
	OUTCOME("outcome")
}
