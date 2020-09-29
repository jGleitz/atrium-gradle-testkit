import ch.tutteli.atrium.api.fluent.en_GB.contains
import ch.tutteli.atrium.api.verbs.expect
import de.joshuagleitze.test.gradle.output
import de.joshuagleitze.test.gradle.task
import de.joshuagleitze.test.gradle.wasInvoked
import de.joshuagleitze.test.gradle.wasSuccessful
import de.joshuagleitze.test.spek.testfiles.testFiles
import org.gradle.testkit.runner.GradleRunner
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.SCOPE
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Files.createDirectories

object KotlinPluginSpek: Spek({
	val testFiles = testFiles()
	val projectDir by memoized(SCOPE) { testFiles.createDirectory("testProject") }

	describe("run") {
		it("compiles the Kotlin code and runs it") {
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

	beforeGroup {
		projectDir.resolve("settings.gradle.kts").toFile().writeText(
			"""
			rootProject.name = "testProject"
			""".trimIndent()
		)
		projectDir.resolve("build.gradle.kts").toFile().writeText(
			"""
			plugins {
				application
				kotlin("jvm") version "1.4.10"
			}
			
			repositories {
				mavenCentral()
			}
			
			application {
				mainClassName = "de.joshuagleitze.HelloWorldKt"
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
	}
})
