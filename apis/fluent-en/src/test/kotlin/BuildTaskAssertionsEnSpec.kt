package de.joshuagleitze.test.gradle

public class BuildTaskAssertionsEnSpec: BuildTaskAssertionsSpec(
	{ wasInvoked() },
	{ assertionCreator -> wasInvoked(assertionCreator) },
	{ wasNotInvoked() },
	{ wasSuccessful() },
	{ failed() },
	{ wasUpToDate() },
	{ wasSkipped() },
	{ usedCachedResult() },
	{ hadNoSource() }
)
