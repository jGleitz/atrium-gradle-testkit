package de.joshuagleitze.test.gradle

import ch.tutteli.atrium.api.fluent.en_GB.isA
import ch.tutteli.atrium.api.fluent.en_GB.messageContains
import ch.tutteli.atrium.api.fluent.en_GB.notToThrow
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.specs.Feature0
import ch.tutteli.atrium.specs.Fun1
import ch.tutteli.atrium.specs.forSubjectLess
import ch.tutteli.atrium.specs.lambda
import ch.tutteli.atrium.specs.name
import ch.tutteli.atrium.specs.unifySignatures
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions.INVOKED
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions.WAS
import de.joshuagleitze.test.gradle.translation.en.BuildTaskAssertions.WAS_NOT
import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import java.nio.file.Files

abstract class BuildTaskAssertionsSpec(
	wasInvokedFeature: Feature0<BuildTask?, BuildTask>,
	wasInvokedFun: Fun1<BuildTask?, Expect<BuildTask>.() -> Unit>,
	wasNotInvoked: Feature0<BuildTask?, Unit>,
	wasSuccessful: Feature0<BuildTask?, BuildTask>,
	failed: Feature0<BuildTask?, BuildTask>,
	wasUpToDate: Feature0<BuildTask?, BuildTask>,
	wasSkipped: Feature0<BuildTask?, BuildTask>,
	usedCachedResult: Feature0<BuildTask?, BuildTask>,
	hadNoSource: Feature0<BuildTask?, BuildTask>
): FunSpec({
	val projectFolder = Files.createTempDirectory("atrium-gradle-testkit-build-task-")
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
			val clean by tasks.registering(Delete::class) {
				delete.add(buildDir)
			}
			
			val successTask by tasks.registering {
				doLast { /* some action */ }
			}
			
			val upToDateTask by tasks.registering {
				outputs.upToDateWhen { true }
			}
			
			val skippedTask by tasks.registering {
				onlyIf { false }
			}
			
			val cachedTask by tasks.registering() {
				outputs.dir(buildDir.resolve("cached"))
				outputs.cacheIf { true }
				doLast { /* some action */ }
			}
			
			val noSourceTask by tasks.registering {
				inputs.files(fileTree(projectDir) {
					exclude("**/*")
				}).skipWhenEmpty().ignoreEmptyDirectories()
				doLast { /* some action */ }
			}
			
			val failureTask by tasks.registering {
				mustRunAfter(successTask, upToDateTask, skippedTask, cachedTask, noSourceTask)
				doLast { error("I want to fail") }
			}
			
			val allTasks by tasks.registering {
				dependsOn(successTask, upToDateTask, skippedTask, cachedTask, noSourceTask, failureTask)
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

	val gradleRun by lazy {
		// create the cache
		runGradle(projectFolder, "cachedTask", "--build-cache")
		// clean so the cache will be used
		runGradle(projectFolder, "clean", "allTasks", "--build-cache", fail = true)
	}

	registerSubjectLessSpec<BuildTask?>(
		"",
		wasInvokedFeature.forSubjectLess(),
		wasInvokedFun.forSubjectLess { isA<BuildTask>() },
		wasNotInvoked.forSubjectLess(),
		wasSuccessful.forSubjectLess(),
		failed.forSubjectLess(),
		wasUpToDate.forSubjectLess(),
		wasSkipped.forSubjectLess(),
		usedCachedResult.forSubjectLess(),
		hadNoSource.forSubjectLess()
	)

	unifySignatures(wasInvokedFeature, wasInvokedFun).forEach { (name, wasInvokedLambda, _) ->
		context(name) {
			test("succeeds if the task was invoked directly") {
				expect {
					expect(gradleRun.task(":clean")).wasInvokedLambda { isA<BuildTask>() }
				}.notToThrow()
			}

			test("succeeds if the task was invoked transitively") {
				expect {
					expect(gradleRun.task(":successTask")).wasInvokedLambda { isA<BuildTask>() }
				}.notToThrow()
			}

			test("fails if the task was not invoked") {
				expect {
					expect(gradleRun.task(":iDontExist")).wasInvokedLambda { isA<BuildTask>() }
				}.toThrow<AssertionError>().messageContains(WAS.getDefault(), INVOKED.getDefault())
			}
		}
	}

	context(wasNotInvoked.name) {
		val wasNotInvokedLambda = wasNotInvoked.lambda

		test("fails if the task was invoked directly") {
			expect {
				expect(gradleRun.task(":clean")).wasNotInvokedLambda()
			}.toThrow<AssertionError>().messageContains(WAS_NOT.getDefault(), INVOKED.getDefault())
		}

		test("fails if the task was invoked transitively") {
			expect {
				expect(gradleRun.task(":successTask")).wasNotInvokedLambda()
			}.toThrow<AssertionError>().messageContains(WAS_NOT.getDefault(), INVOKED.getDefault())
		}

		test("succeeds if the task was not invoked") {
			expect {
				expect(gradleRun.task(":iDontExist")).wasNotInvokedLambda()
			}.notToThrow()
		}
	}

	data class OutcomeCheck(val feature: Feature0<BuildTask?, BuildTask>, val outcome: TaskOutcome, val task: String)

	val outcomeChecks = listOf(
		OutcomeCheck(wasSuccessful, outcome = SUCCESS, task = ":successTask"),
		OutcomeCheck(failed, outcome = FAILED, task = ":failureTask"),
		OutcomeCheck(wasUpToDate, outcome = UP_TO_DATE, task = ":upToDateTask"),
		OutcomeCheck(wasSkipped, outcome = SKIPPED, task = ":skippedTask"),
		OutcomeCheck(usedCachedResult, outcome = FROM_CACHE, ":cachedTask"),
		OutcomeCheck(hadNoSource, outcome = NO_SOURCE, ":noSourceTask")
	)
	outcomeChecks.forEach { (feature, outcome, task) ->
		val featureFun = feature.lambda

		context(feature.name) {
			test("succeeds if the outcome is $outcome") {
				expect {
					expect(gradleRun.task(task)).featureFun()
				}.notToThrow()
			}

			outcomeChecks.filter { it.outcome != outcome }.forEach { (_, otherOutcome, otherTask) ->
				test("fails if the outcome is $otherOutcome") {
					expect {
						expect(gradleRun.task(otherTask)).featureFun()
					}.toThrow<AssertionError>().messageContains(outcome.name, otherOutcome.name)
				}
			}

			test("fails if the task was not invoked") {
				expect {
					expect(gradleRun.task("iDontExist")).featureFun()
				}.toThrow<AssertionError>().messageContains(outcome.name, WAS.getDefault(), INVOKED.getDefault())
			}
		}
	}
})
