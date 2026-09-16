import ch.tutteli.atrium.api.fluent.en_GB.contains
import ch.tutteli.atrium.api.verbs.expect
import de.joshuagleitze.test.gradle.output
import de.joshuagleitze.test.gradle.task
import de.joshuagleitze.test.gradle.wasInvoked
import de.joshuagleitze.test.gradle.wasSuccessful
import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import java.nio.file.Files.createDirectories

class KotlinPluginSpec: FunSpec({
	val projectDir = Files.createTempDirectory("atrium-gradle-testkit-example-")
	var setupCompleted = false
	var testsSuccessful = true

	beforeSpec {
		projectDir.resolve("settings.gradle.kts").toFile().writeText(
			"""
			rootProject.name = "testProject"
			""".trimIndent()
		)
		projectDir.resolve("build.gradle.kts").toFile().writeText(
			"""
			plugins {
				application
				kotlin("jvm") version "2.4.20"
			}
			
			repositories {
				mavenCentral()
			}
			
			application {
				mainClass.set("de.joshuagleitze.HelloWorldKt")
			}
			""".trimIndent()
		)
		createDirectories(projectDir.resolve("src/main/kotlin")).resolve("HelloWorld.kt").toFile().writeText(
			"""
			package de.joshuagleitze
			
			fun main() {
				println("Hello World!")
			}
			""".trimIndent()
		)
		setupCompleted = true
	}

	afterTest { (_, result) ->
		if (result.isErrorOrFailure) {
			testsSuccessful = false
		}
	}

	afterSpec {
		if (setupCompleted && testsSuccessful) {
			check(projectDir.toFile().deleteRecursively()) {
				"Failed to delete temporary project directory: $projectDir"
			}
		}
	}

	context("run") {
		test("compiles the Kotlin code and runs it") {
			val runResult = GradleRunner.create()
				.forwardOutput()
				.withProjectDir(projectDir.toFile())
				.withArguments("run")
				.build()

			expect(runResult) {
				task(":compileKotlin").wasSuccessful()
				task(":classes").wasInvoked()
				output.contains("Hello World!")
			}
		}
	}
})
