package de.joshuagleitze.test.gradle

public class BuildTaskAssertionsEnSpec: BuildTaskAssertionsSpec(
	{ toBeInvoked() },
	{ assertionCreator -> toBeInvoked(assertionCreator) },
	{ notToBeInvoked() },
	{ outcome -> toHaveOutcome(outcome) },
	{ toBeSuccessful() },
	{ toHaveFailed() },
	{ toBeUpToDate() },
	{ toBeSkipped() },
	{ toHaveBeenLoadedFromCache() },
	{ toHaveNoSource() }
)
