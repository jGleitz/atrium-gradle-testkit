package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.api.fluent.en_GB.contains
import ch.tutteli.atrium.api.fluent.en_GB.messageContains
import ch.tutteli.atrium.api.fluent.en_GB.notToBeNull
import ch.tutteli.atrium.api.fluent.en_GB.notToThrow
import ch.tutteli.atrium.api.fluent.en_GB.toBe
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.specs.Feature0
import ch.tutteli.atrium.specs.Feature1
import ch.tutteli.atrium.specs.Fun1
import ch.tutteli.atrium.specs.forSubjectLess
import ch.tutteli.atrium.specs.lambda
import ch.tutteli.atrium.specs.name
import ch.tutteli.atrium.specs.unifySignatures
import de.joshuagleitze.test.gradle.translation.en.BuildResultAssertions.OUTPUT
import de.joshuagleitze.test.gradle.translation.en.BuildResultAssertions.TASK
import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import java.nio.file.Files

abstract class BuildResultAssertionsSpec(
	outputFeature: Feature0<BuildResult, String>,
	outputFun: Fun1<BuildResult, Expect<String>.() -> Unit>,
	taskFeature: Feature1<BuildResult, String, BuildTask?>
): FunSpec({
	val projectFolder = Files.createTempDirectory("atrium-gradle-testkit-build-result-")
	var setupCompleted = false
	var testsSuccessful = true

	beforeSpec {
		projectFolder.resolve("settings.gradle.kts").toFile().writeText(
			"""
			rootProject.name = "testProject"
			""".trimIndent()
		)
		projectFolder.resolve("build.gradle.kts").toFile().writeText(
			"""
			val logHamlet by tasks.registering {
				doLast {
					println(
						${"\""}""
						To be, or not to be, that is the question:
						Whether 'tis nobler in the mind to suffer
						The slings and arrows of outrageous fortune,
						Or to take arms against a sea of troubles
						And by opposing end them.
						${"\""}"".trimIndent()
					)
				}
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
			check(projectFolder.toFile().deleteRecursively()) {
				"Failed to delete temporary project directory: $projectFolder"
			}
		}
	}

	val loggingTaskResult by lazy { runGradle(projectFolder, "logHamlet") }

	registerSubjectLessSpec<BuildResult>(
		"",
		outputFeature.forSubjectLess(),
		outputFun.forSubjectLess { toBe("irrelevant") },
		taskFeature.forSubjectLess("irrelevant")
	)

	unifySignatures(outputFeature, outputFun).forEach { (name, outputFun, _) ->
		context(name) {
			test("allows to check the build output") {
				expect {
					expect(loggingTaskResult).outputFun {
						contains("To be, or not to be, that is the question:")
					}
				}.notToThrow()
			}

			test("describes the output feature") {
				expect {
					expect(loggingTaskResult).outputFun {
						contains("the answer to the ultimate question of life, the universe, and everything")
					}
				}.toThrow<AssertionError>().messageContains("${OUTPUT.getDefault()}:", "To be, or not to be, that is the question:")
			}
		}
	}

	context(taskFeature.name) {
		val taskFun = taskFeature.lambda

		test("allows to check an existing task") {
			expect {
				expect(loggingTaskResult).taskFun(":logHamlet").notToBeNull()
			}.notToThrow()
		}

		test("describes an existing task") {
			expect {
				expect(loggingTaskResult).taskFun(":logHamlet").toBe(null)
			}.toThrow<AssertionError>().messageContains("${TASK.getDefault()} ':logHamlet'")
		}

		test("allows to check for a non-existent task") {
			expect {
				expect(loggingTaskResult).taskFun(":iDontExist").toBe(null)
			}.notToThrow()
		}

		test("describes a non-existent task") {
			expect {
				expect(loggingTaskResult).taskFun(":iDontExist").notToBeNull()
			}.toThrow<AssertionError>().messageContains("${TASK.getDefault()} ':iDontExist': null")
		}
	}
})
