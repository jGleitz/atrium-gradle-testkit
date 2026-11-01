package de.joshuagleitze.test.gradle

public class BuildResultAssertionsEnSpec: BuildResultAssertionsSpec(
	{ output },
	{ assertionCreator -> output(assertionCreator) },
	{ path -> task(path) },
	{ path, assertionCreator -> task(path, assertionCreator) }
)
