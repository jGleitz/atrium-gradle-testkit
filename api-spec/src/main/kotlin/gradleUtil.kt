package de.joshuagleitze.test.gradle

import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path

internal fun runGradle(projectFolder: Path, vararg commands: String, fail: Boolean = false) =
	GradleRunner.create()
		.forwardOutput()
		.withProjectDir(projectFolder.toFile())
		.withArguments(*commands)
		.run {
			if (fail) buildAndFail()
			else build()
		}
