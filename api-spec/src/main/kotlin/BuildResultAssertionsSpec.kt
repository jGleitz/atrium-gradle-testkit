package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.api.fluent.en_GB.messageToContain
import ch.tutteli.atrium.api.fluent.en_GB.notToEqualNull
import ch.tutteli.atrium.api.fluent.en_GB.notToThrow
import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import ch.tutteli.atrium.creating.Expect
import de.joshuagleitze.test.gradle.translation.en.BuildResultAssertions.OUTPUT
import de.joshuagleitze.test.gradle.translation.en.BuildResultAssertions.TASK
import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask

/**
 * The behaviour every API must provide for assertions on a [BuildResult]. Concrete API modules provide their
 * assertion functions as lambdas.
 */
abstract class BuildResultAssertionsSpec(
	outputFeature: Expect<BuildResult>.() -> Expect<String>,
	outputWithCreator: Expect<BuildResult>.(Expect<String>.() -> Unit) -> Expect<BuildResult>,
	taskFeature: Expect<BuildResult>.(String) -> Expect<BuildTask?>
): FunSpec({
	val fixtures = TemporaryFixtures(this, "atrium-gradle-testkit-build-result-")
	lateinit var loggingTaskResult: BuildResult

	beforeSpec {
		val projectDir = fixtures.newDirectory("logging")
		projectDir.writeGradleProject(
			"""
			tasks.register("logHamlet") {
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
		loggingTaskResult = runGradle(projectDir, "logHamlet")
		fixtures.setupCompleted()
	}

	registerSubjectLessTests(
		listOf(
			// since Atrium 0.17 (robstoll/atrium#950), a feature without subject and without subsequent expectations is not
			// reported on its own, so the feature-only overloads are checked together with an expectation on the feature
			SubjectLessCase<BuildResult>("output", listOf("${OUTPUT.getDefault()}:", "irrelevant")) {
				outputFeature().toEqual("irrelevant")
			},
			SubjectLessCase<BuildResult>("output { }", listOf("${OUTPUT.getDefault()}:", "irrelevant")) {
				outputWithCreator { toEqual("irrelevant") }
			},
			SubjectLessCase<BuildResult>("task(path)", listOf("${TASK.getDefault()} 'irrelevant':")) {
				taskFeature("irrelevant").toEqual(null)
			}
		)
	)

	/** Both overloads of the `output` assertion, reduced to “assert that the output contains the given text”. */
	data class OutputOverload(val name: String, val assertOutputContains: Expect<BuildResult>.(String) -> Unit)

	val outputOverloads = listOf(
		OutputOverload("output") { text -> outputFeature().toContain(text) },
		OutputOverload("output { }") { text -> outputWithCreator { toContain(text) } }
	)

	outputOverloads.forEach { (name, assertOutputContains) ->
		context(name) {
			test("allows to check the build output") {
				expect {
					expect(loggingTaskResult).assertOutputContains("To be, or not to be, that is the question:")
				}.notToThrow()
			}

			test("describes the output feature") {
				expect {
					expect(loggingTaskResult).assertOutputContains("the answer to the ultimate question of life, the universe, and everything")
				}.toThrow<AssertionError>().messageToContain("${OUTPUT.getDefault()}:", "To be, or not to be, that is the question:")
			}
		}
	}

	context("output { }") {
		test("fails if the assertionCreator does not create an assertion") {
			expect {
				expect(loggingTaskResult).outputWithCreator { }
			}.toThrow<AssertionError>().messageToContain("${OUTPUT.getDefault()}:", "at least one assertion defined")
		}
	}

	context("task(path)") {
		test("allows to check an existing task") {
			expect {
				expect(loggingTaskResult).taskFeature(":logHamlet").notToEqualNull()
			}.notToThrow()
		}

		test("describes an existing task") {
			expect {
				expect(loggingTaskResult).taskFeature(":logHamlet").toEqual(null)
			}.toThrow<AssertionError>().messageToContain("${TASK.getDefault()} ':logHamlet'")
		}

		test("allows to check for a non-existent task") {
			expect {
				expect(loggingTaskResult).taskFeature(":iDontExist").toEqual(null)
			}.notToThrow()
		}

		test("describes a non-existent task") {
			expect {
				expect(loggingTaskResult).taskFeature(":iDontExist").notToEqualNull()
			}.toThrow<AssertionError>().messageToContain("${TASK.getDefault()} ':iDontExist': null")
		}
	}
})
