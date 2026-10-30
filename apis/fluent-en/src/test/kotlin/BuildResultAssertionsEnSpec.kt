package de.joshuagleitze.test.gradle

import org.gradle.testkit.runner.BuildResult

public class BuildResultAssertionsEnSpec: BuildResultAssertionsSpec(
	{ output },
	{ assertionCreator -> output(assertionCreator) },
	{ path -> task(path) }
)
