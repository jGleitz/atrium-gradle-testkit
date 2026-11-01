package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * Proves that the shared suite ran with the selected runtime: the TestKit client of the selected Gradle distribution,
 * fixtures on that distribution, JVM 21 for the test worker and the fixtures, and the staged library. Records the
 * runtime in a machine-readable report.
 */
class RuntimeProvenanceSpec : FunSpec({
	val fixtures = TemporaryFixtures(this, "atrium-gradle-testkit-runtime-provenance-")

	test("the shared suite runs with the selected TestKit client, fixture runtime and staged library") {
		val selectedGradleVersion = requiredProperty("compatibilityGradleVersion")
		val daemonGradleHome = Path.of(requiredProperty("compatibilityDaemonGradleHome")).toRealPath()
		val distributionTestKitJar = Path.of(requiredProperty("compatibilityTestKitJar")).toRealPath()
		val testedLibraryVersion = requiredProperty("testedLibraryVersion")
		val expectedLibraryJar = Path.of(requiredProperty("stagedRepository"))
			.resolve("de/joshuagleitze/atrium-gradle-testkit/$testedLibraryVersion")
			.resolve("atrium-gradle-testkit-$testedLibraryVersion.jar")

		val clientSources = listOf(GradleRunner::class.java, BuildResult::class.java, BuildTask::class.java)
			.associate { it.name to codeSource(it) }
		val libraryJar = codeSource(Class.forName("de.joshuagleitze.test.gradle.BuildResultFeatureExtractorsKt"))

		val projectDir = fixtures.newDirectory("runtime")
		projectDir.writeGradleProject(
			"""
			tasks.register("reportRuntime") {
				val gradleVersion = gradle.gradleVersion
				doLast {
					println("FIXTURE-GRADLE-VERSION=" + gradleVersion)
					println("FIXTURE-JAVA-HOME=" + System.getProperty("java.home"))
					println("FIXTURE-JAVA-VERSION=" + System.getProperty("java.version"))
				}
			}
			""".trimIndent()
		)
		fixtures.setupCompleted()
		val fixtureOutput = runGradle(projectDir, "reportRuntime").output.lines()
		fun fixtureValue(key: String) = fixtureOutput.single { it.startsWith("$key=") }.substringAfter("=")

		val testJavaHome = Path.of(System.getProperty("java.home")).toRealPath()
		val runtime = linkedMapOf(
			"selectedGradleVersion" to selectedGradleVersion,
			"daemonGradleVersion" to requiredProperty("compatibilityDaemonGradleVersion"),
			"daemonGradleHome" to daemonGradleHome.toString(),
			"distributionTestKitJar" to distributionTestKitJar.toString(),
			"daemonJavaHome" to requiredProperty("compatibilityDaemonJavaHome"),
			"daemonJavaVersion" to requiredProperty("compatibilityDaemonJavaVersion"),
			"testJavaHome" to testJavaHome.toString(),
			"testJavaVersion" to System.getProperty("java.version"),
			"fixtureGradleVersion" to fixtureValue("FIXTURE-GRADLE-VERSION"),
			"fixtureJavaHome" to fixtureValue("FIXTURE-JAVA-HOME"),
			"fixtureJavaVersion" to fixtureValue("FIXTURE-JAVA-VERSION"),
			"testKitClientCodeSources" to clientSources.mapValues { it.value.toString() },
			"libraryVersion" to testedLibraryVersion,
			"libraryJar" to libraryJar.toString(),
			"libraryJarSha256" to sha256(libraryJar),
		)
		Path.of(requiredProperty("compatibilityRuntimeReport")).also { Files.createDirectories(it.parent) }
			.toFile().writeText(toJson(runtime) + "\n")

		expect(runtime["daemonGradleVersion"]).toEqual(selectedGradleVersion)
		clientSources.forEach { (type, source) ->
			println("TESTKIT-CLIENT $type=$source")
			expect(source).toEqual(distributionTestKitJar)
			expect(source.fileName.toString()).toEqual("gradle-test-kit-$selectedGradleVersion.jar")
		}
		expect(Runtime.version().feature()).toEqual(21)
		expect(Runtime.Version.parse(requiredProperty("compatibilityDaemonJavaVersion")).feature()).toEqual(21)
		expect(runtime["fixtureGradleVersion"]).toEqual(selectedGradleVersion)
		expect(Path.of(fixtureValue("FIXTURE-JAVA-HOME")).toRealPath()).toEqual(testJavaHome)
		expect(Runtime.Version.parse(fixtureValue("FIXTURE-JAVA-VERSION")).feature()).toEqual(21)
		expect(libraryJar).toEqual(expectedLibraryJar.toRealPath())
	}
})

private fun requiredProperty(name: String): String =
	checkNotNull(System.getProperty(name)) { "The system property '$name' is required." }

private fun codeSource(type: Class<*>): Path = Path.of(type.protectionDomain.codeSource.location.toURI()).toRealPath()

private fun sha256(file: Path): String =
	MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)).joinToString("") { "%02x".format(it) }

private fun toJson(value: Any?): String = when (value) {
	is Map<*, *> -> value.entries.joinToString(",\n", "{\n", "\n}") { (key, entry) ->
		"  ${toJson(key)}: ${toJson(entry).replace("\n", "\n  ")}"
	}
	is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
	else -> error("Unsupported JSON value: $value")
}
