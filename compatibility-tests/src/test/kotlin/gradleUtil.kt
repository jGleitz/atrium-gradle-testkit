package de.joshuagleitze.test.gradle

import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/**
 * Runs a fresh [GradleRunner] for every invocation; runners are never shared. Fixture builds must be free of
 * deprecations and build script compiler warnings, so that they keep working on future Gradle versions.
 */
internal fun runGradle(projectFolder: Path, vararg commands: String, fail: Boolean = false): BuildResult {
	val result = GradleRunner.create()
		.forwardOutput()
		.withProjectDir(projectFolder.toFile())
		.withSelectedRuntime()
		.withArguments(*commands, "--warning-mode=fail", "--console=plain")
		.run {
			if (fail) buildAndFail()
			else build()
		}
	println(
		"FIXTURE-RUN dir=$projectFolder arguments=${commands.joinToString(" ")} outcomes=" +
			result.tasks.joinToString(",") { "${it.path}=${it.outcome}" }
	)
	val compilerWarnings = result.output.lines().filter { it.startsWith("w: ") }
	check(compilerWarnings.isEmpty()) { "The fixture build in $projectFolder has compiler warnings:\n${compilerWarnings.joinToString("\n")}" }
	check("Deprecated Gradle features" !in result.output) { "The fixture build in $projectFolder used deprecated Gradle features" }
	return result
}

/**
 * In the standalone compatibility build, fixtures run with the selected Gradle distribution and the JVM of the test
 * worker, in a TestKit directory of their own. The host build selects no runtime and uses TestKit's defaults.
 */
private fun GradleRunner.withSelectedRuntime(): GradleRunner {
	val gradleVersion = System.getProperty("compatibilityGradleVersion") ?: return this
	val testKitDir = Path.of(checkNotNull(System.getProperty("compatibilityTestKitDir")) { "compatibilityTestKitDir" })
	Files.createDirectories(testKitDir)
	// The TestKit directory is the fixtures' Gradle user home, so its properties apply before the daemon is selected.
	val properties = Properties().apply { setProperty("org.gradle.java.home", System.getProperty("java.home")) }
	Files.newBufferedWriter(testKitDir.resolve("gradle.properties")).use { properties.store(it, null) }
	return withGradleVersion(gradleVersion).withTestKitDir(testKitDir.toFile())
}

internal fun Path.writeGradleProject(buildScript: String, settingsScript: String = "") {
	resolve("settings.gradle.kts").toFile().writeText("rootProject.name = \"testProject\"\n$settingsScript")
	resolve("build.gradle.kts").toFile().writeText(buildScript)
}

/**
 * Provides unique fixture project directories below one temporary root directory per spec. The root is deleted after
 * the spec if all tests succeeded; a failed deletion then fails the spec. If any test or the fixture setup failed, the
 * root is retained for inspection and its path is printed.
 */
internal class TemporaryFixtures(spec: FunSpec, prefix: String) {
	val root: Path = Files.createTempDirectory(prefix)
	private var setupCompleted = false
	private var testsSuccessful = true

	init {
		println("FIXTURE-ROOT $root")
		spec.afterTest { (_, result) ->
			if (result.isErrorOrFailure) testsSuccessful = false
		}
		spec.afterSpec {
			if (setupCompleted && testsSuccessful) {
				check(root.toFile().deleteRecursively()) { "Failed to delete temporary fixture directory: $root" }
			} else {
				println("FIXTURE-RETAINED $root (setup completed: $setupCompleted, tests successful: $testsSuccessful)")
			}
		}
	}

	/** Creates a new directory below the root; fails if [name] was already used. */
	fun newDirectory(name: String): Path = Files.createDirectory(root.resolve(name))

	fun setupCompleted() {
		setupCompleted = true
	}
}
